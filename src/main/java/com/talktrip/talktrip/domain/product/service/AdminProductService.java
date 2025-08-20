package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductCreateRequest;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductUpdateRequest;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductEditResponse;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.repository.*;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.exception.*;
import com.talktrip.talktrip.global.repository.CountryRepository;
import com.talktrip.talktrip.global.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CountryRepository countryRepository;
    private final MemberRepository memberRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductHashTagRepository productHashTagRepository;
    private final ProductOptionRepository productOptionRepository;
    private final S3Uploader s3Uploader;

    @Transactional
    public void createProduct(AdminProductCreateRequest request, Long memberId,
                              MultipartFile thumbnailImage, List<MultipartFile> detailImages) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorCode.ADMIN_NOT_FOUND));

        Country country = countryRepository.findByName(request.countryName())
                .orElseThrow(() -> new ProductException(ErrorCode.COUNTRY_NOT_FOUND));

        Product product = request.to(member, country);

        // 썸네일
        if (thumbnailImage != null && !thumbnailImage.isEmpty()) {
            String thumbnailUrl = s3Uploader.upload(thumbnailImage, "products/thumbnail");
            String thumbnailHash = s3Uploader.calculateHash(thumbnailImage);
            product.updateThumbnailImage(thumbnailUrl, thumbnailHash);
        }

        product.updateBasicInfo(request.productName(), request.description(), country);

        // 상세 이미지: 들어온 순서대로 sortOrder 부여
        if (detailImages != null && !detailImages.isEmpty()) {
            int order = 0;
            for (MultipartFile file : detailImages) {
                String url = s3Uploader.upload(file, "products/detail");
                product.getImages().add(
                        ProductImage.builder()
                                .product(product)
                                .imageUrl(url)
                                .sortOrder(order++)
                                .build()
                );
            }
        }

        product.getHashtags().addAll(request.toHashTags(product));
        product.getProductOptions().addAll(request.toProductOptions(product));

        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<AdminProductSummaryResponse> getMyProducts(
            Long memberId,
            String keyword,
            String status,
            Pageable pageable
    ) {
        String st = (status == null || status.isBlank()) ? "ACTIVE" : status.trim();
        Page<Product> page = productRepository.findSellerProducts(memberId, st, keyword, pageable);

        return page.map(AdminProductSummaryResponse::from);
    }

    @Transactional
    public AdminProductEditResponse getMyProductEditForm(Long productId, Long memberId) {
        Product product = productRepository.findByIdIncludingDeleted(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getMember().getId().equals(memberId)) {
            throw new ProductException(ErrorCode.ACCESS_DENIED);
        }
        return AdminProductEditResponse.from(product);
    }


    // detailImageOrder 예: ["id:12","new:0","id:15","new:1"]
    // id:X  : 기존 이미지 X를 유지하며 새 sortOrder로 재생성(파일은 유지, 엔티티만 새로)
    // new:N : 신규 파일 리스트(detailImages)의 N번 파일 업로드하여 해당 위치에 배치
    @Transactional
    public void updateProduct(Long productId,
                              AdminProductUpdateRequest request,
                              Long memberId,
                              MultipartFile thumbnailImage,
                              List<MultipartFile> detailImages,
                              List<String> detailImageOrder) {

        Product product = productRepository.findByIdIncludingDeleted(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getMember().getId().equals(memberId)) {
            throw new ProductException(ErrorCode.ACCESS_DENIED);
        }

        Country country = countryRepository.findByName(request.countryName())
                .orElseThrow(() -> new ProductException(ErrorCode.COUNTRY_NOT_FOUND));

        // 썸네일
        boolean thumbnailDeleted = (thumbnailImage == null && request.existingThumbnailHash() == null);
        if (thumbnailDeleted) {
            if (product.getThumbnailImageUrl() != null) {
                s3Uploader.deleteFile(product.getThumbnailImageUrl());
            }
            product.updateThumbnailImage(null, null);
        } else if (thumbnailImage != null && !thumbnailImage.isEmpty()) {
            String newHash = s3Uploader.calculateHash(thumbnailImage);
            if (!newHash.equals(product.getThumbnailImageHash())) {
                if (product.getThumbnailImageUrl() != null) s3Uploader.deleteFile(product.getThumbnailImageUrl());
                String uploadedUrl = s3Uploader.upload(thumbnailImage, "products/thumbnail");
                product.updateThumbnailImage(uploadedUrl, newHash);
            }
        }

        // 현재 이미지 맵
        List<ProductImage> current = productImageRepository.findAllByProduct(product);
        Map<Long, ProductImage> byId = current.stream()
                .collect(Collectors.toMap(ProductImage::getId, Function.identity()));

        // 신규 파일 리스트
        List<MultipartFile> newFiles = (detailImages != null) ? detailImages : List.of();

        // detailImageOrder 없으면: 기존 로직(fallback) - 기존 유지 ID 순서 + 신규 뒤에
        if (detailImageOrder == null || detailImageOrder.isEmpty()) {
            // 기존 요청 DTO의 existingDetailImageIds를 사용(없으면 빈 리스트)
            List<Long> keepOrder = Optional.ofNullable(request.existingDetailImageIds()).orElse(List.of());
            Set<Long> keepSet = new HashSet<>(keepOrder);

            // 제거 대상: keep 밖의 기존 → S3 삭제 + DB 삭제
            for (ProductImage img : current) {
                if (!keepSet.contains(img.getId())) {
                    s3Uploader.deleteFile(img.getImageUrl());
                    productImageRepository.delete(img);
                }
            }
            product.getImages().clear();

            // 최종 순서대로 재생성
            int ord = 0;
            // 1) 기존 유지분(요청 순서)
            for (Long id : keepOrder) {
                ProductImage exist = byId.get(id);
                if (exist != null) {
                    product.getImages().add(ProductImage.builder()
                            .product(product)
                            .imageUrl(exist.getImageUrl())
                            .sortOrder(ord++)
                            .build());
                }
            }
            // 2) 신규 이미지(업로드 순서)
            for (MultipartFile file : newFiles) {
                String url = s3Uploader.upload(file, "products/detail");
                product.getImages().add(ProductImage.builder()
                        .product(product)
                        .imageUrl(url)
                        .sortOrder(ord++)
                        .build());
            }

        } else {
            // 메인 시나리오: 최종 순서 토큰 처리
            // 1) 이번 요청에서 참조되지 않은 기존 이미지는 제거(S3+DB)
            Set<Long> referencedIds = detailImageOrder.stream()
                    .filter(tok -> tok.startsWith("id:"))
                    .map(tok -> Long.parseLong(tok.substring(3)))
                    .collect(Collectors.toSet());

            for (ProductImage img : current) {
                if (!referencedIds.contains(img.getId())) {
                    s3Uploader.deleteFile(img.getImageUrl());
                    productImageRepository.delete(img);
                }
            }
            product.getImages().clear(); // 나머지는 재생성할 것이므로 비움

            // 2) 토큰 순서대로 최종 리스트 재생성
            boolean[] newUsed = new boolean[newFiles.size()];
            int ord = 0;

            for (String token : detailImageOrder) {
                if (token.startsWith("id:")) {
                    Long id = Long.parseLong(token.substring(3));
                    ProductImage exist = byId.get(id);
                    if (exist != null) {
                        product.getImages().add(ProductImage.builder()
                                .product(product)
                                .imageUrl(exist.getImageUrl()) // 파일은 유지
                                .sortOrder(ord++)
                                .build());
                    }
                } else if (token.startsWith("new:")) {
                    int idx = Integer.parseInt(token.substring(4));
                    if (idx < 0 || idx >= newFiles.size() || newUsed[idx]) {
                        throw new ProductException(ErrorCode.IMAGE_UPLOAD_FAILED);
                    }
                    newUsed[idx] = true;
                    MultipartFile file = newFiles.get(idx);
                    if (file == null || file.isEmpty()) {
                        throw new ProductException(ErrorCode.IMAGE_UPLOAD_FAILED);
                    }
                    String url = s3Uploader.upload(file, "products/detail");
                    product.getImages().add(ProductImage.builder()
                            .product(product)
                            .imageUrl(url)
                            .sortOrder(ord++)
                            .build());
                } else {
                    throw new ProductException(ErrorCode.IMAGE_UPLOAD_FAILED);
                }
            }
        }

        product.updateBasicInfo(request.productName(), request.description(), country);

        productHashTagRepository.deleteAllByProduct(product);
        product.getHashtags().clear();
        product.getHashtags().addAll(request.toHashTags(product));

        productOptionRepository.deleteAllByProduct(product);
        product.getProductOptions().clear();
        product.getProductOptions().addAll(request.toProductOptions(product));
    }

    @Transactional
    public void deleteProduct(Long productId, Long memberId) {
        Product product = productRepository.findByIdIncludingDeleted(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getMember().getId().equals(memberId)) {
            throw new ProductException(ErrorCode.ACCESS_DENIED);
        }

        product.markDeleted();
    }

    @Transactional
    public void restoreProduct(Long productId, Long memberId) {
        Product product = productRepository.findByIdIncludingDeleted(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getMember().getId().equals(memberId)) {
            throw new ProductException(ErrorCode.ACCESS_DENIED);
        }

        product.restore();
    }
}
