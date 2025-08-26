package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductCreateRequest;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductUpdateRequest;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductEditResponse;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.repository.ProductHashTagRepository;
import com.talktrip.talktrip.domain.product.repository.ProductImageRepository;
import com.talktrip.talktrip.domain.product.repository.ProductOptionRepository;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.repository.CountryRepository;
import com.talktrip.talktrip.global.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private static final String THUMBNAIL_PATH = "products/thumbnail";
    private static final String DETAIL_PATH = "products/detail";
    private static final String ID_PREFIX = "id:";
    private static final String NEW_PREFIX = "new:";

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
        Member member = findAdminMember(memberId);
        Country country = findCountry(request.countryName());
        Product product = request.to(member, country);

        processThumbnailImage(product, thumbnailImage);
        processDetailImages(product, detailImages);
        addProductData(product, request);

        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<AdminProductSummaryResponse> getMyProducts(
            Long memberId,
            String keyword,
            String status,
            Pageable pageable
    ) {
        validateAdminMember(memberId);
        Page<Product> page = productRepository.findSellerProducts(memberId, status, keyword, pageable);

        return page.map(AdminProductSummaryResponse::from);
    }

    @Transactional
    public AdminProductEditResponse getMyProductEditForm(Long productId, Long memberId) {
        validateAdminMember(memberId);
        Product product = findProduct(productId, memberId);
        return AdminProductEditResponse.from(product);
    }

    @Transactional
    public void updateProduct(Long productId,
                              AdminProductUpdateRequest request,
                              Long memberId,
                              MultipartFile thumbnailImage,
                              List<MultipartFile> detailImages,
                              List<String> detailImageOrder) {

        validateAdminMember(memberId);
        Product product = findProduct(productId, memberId);
        Country country = findCountry(request.countryName());

        updateThumbnailImage(product, thumbnailImage, request.existingThumbnailHash());
        updateDetailImages(product, detailImages, detailImageOrder, request.existingDetailImageIds());

        product.updateBasicInfo(request.productName(), request.description(), country);
        updateProductData(product, request);
    }

    @Transactional
    public void deleteProduct(Long productId, Long memberId) {
        validateAdminMember(memberId);
        Product product = findProduct(productId, memberId);
        
        if (product.isDeleted()) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        
        product.markDeleted();
    }

    @Transactional
    public void restoreProduct(Long productId, Long memberId) {
        validateAdminMember(memberId);
        Product product = findProduct(productId, memberId);
        
        if (!product.isDeleted()) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        
        product.restore();
    }

    // 공통 검증 메서드들
    private void validateAdminMember(Long memberId) {
        if (memberId == null) {
            throw new MemberException(ErrorCode.USER_NOT_FOUND);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorCode.USER_NOT_FOUND));
        
        if (!member.getMemberRole().equals(MemberRole.A)) {
            throw new MemberException(ErrorCode.ACCESS_DENIED);
        }
    }

    private Member findAdminMember(Long memberId) {
        if (memberId == null) {
            throw new MemberException(ErrorCode.USER_NOT_FOUND);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorCode.USER_NOT_FOUND));
        
        if (!member.getMemberRole().equals(MemberRole.A)) {
            throw new MemberException(ErrorCode.ACCESS_DENIED);
        }
        
        return member;
    }

    private Country findCountry(String countryName) {
        return countryRepository.findByName(countryName)
                .orElseThrow(() -> new ProductException(ErrorCode.COUNTRY_NOT_FOUND));
    }

    private Product findProduct(Long productId, Long memberId) {
        Product product = findProductById(productId);
        validateProductOwnership(product, memberId);
        return product;
    }

    private Product findProductById(Long productId) {
        Product product = productRepository.findProductWithAllDetailsById(productId);
        if (product == null) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private void validateProductOwnership(Product product, Long memberId) {
        if (!product.getMember().getId().equals(memberId)) {
            throw new MemberException(ErrorCode.ACCESS_DENIED);
        }
    }

    // 이미지 처리 메서드들
    private void processThumbnailImage(Product product, MultipartFile thumbnailImage) {
        if (thumbnailImage != null && !thumbnailImage.isEmpty()) {
            String thumbnailUrl = s3Uploader.upload(thumbnailImage, THUMBNAIL_PATH);
            String thumbnailHash = s3Uploader.calculateHash(thumbnailImage);
            product.updateThumbnailImage(thumbnailUrl, thumbnailHash);
        }
    }

    private void processDetailImages(Product product, List<MultipartFile> detailImages) {
        if (detailImages != null && !detailImages.isEmpty()) {
            for (int i = 0; i < detailImages.size(); i++) {
                MultipartFile file = detailImages.get(i);
                String url = s3Uploader.upload(file, DETAIL_PATH);
                product.getImages().add(
                        ProductImage.builder()
                                .product(product)
                                .imageUrl(url)
                                .sortOrder(i)
                                .build()
                );
            }
        }
    }

    private void addProductData(Product product, AdminProductCreateRequest request) {
        product.getHashtags().addAll(request.toHashTags(product));
        product.getProductOptions().addAll(request.toProductOptions(product));
    }

    private void updateThumbnailImage(Product product, MultipartFile thumbnailImage, String existingThumbnailHash) {
        boolean thumbnailDeleted = (thumbnailImage == null && existingThumbnailHash == null);
        
        if (thumbnailDeleted) {
            deleteThumbnailImage(product);
        } else if (thumbnailImage != null && !thumbnailImage.isEmpty()) {
            updateThumbnailImageWithNewFile(product, thumbnailImage);
        }
    }

    private void deleteThumbnailImage(Product product) {
        if (product.getThumbnailImageUrl() != null) {
            s3Uploader.deleteFile(product.getThumbnailImageUrl());
        }
        product.updateThumbnailImage(null, null);
    }

    private void updateThumbnailImageWithNewFile(Product product, MultipartFile thumbnailImage) {
        String newHash = s3Uploader.calculateHash(thumbnailImage);
        if (!newHash.equals(product.getThumbnailImageHash())) {
            if (product.getThumbnailImageUrl() != null) {
                s3Uploader.deleteFile(product.getThumbnailImageUrl());
            }
            String uploadedUrl = s3Uploader.upload(thumbnailImage, THUMBNAIL_PATH);
            product.updateThumbnailImage(uploadedUrl, newHash);
        }
    }

    private void updateDetailImages(Product product, List<MultipartFile> detailImages, 
                                   List<String> detailImageOrder, List<Long> existingDetailImageIds) {
        List<ProductImage> current = productImageRepository.findAllByProduct(product);
        Map<Long, ProductImage> byId = current.stream()
                .collect(Collectors.toMap(ProductImage::getId, Function.identity()));

        List<MultipartFile> newFiles = (detailImages != null) ? detailImages : List.of();

        if (detailImageOrder == null || detailImageOrder.isEmpty()) {
            updateDetailImagesFallback(product, current, byId, newFiles, existingDetailImageIds);
        } else {
            updateDetailImagesWithOrder(product, current, byId, newFiles, detailImageOrder);
        }
    }

    private void updateDetailImagesFallback(Product product, List<ProductImage> current, 
                                           Map<Long, ProductImage> byId, List<MultipartFile> newFiles, 
                                           List<Long> existingDetailImageIds) {
        List<Long> keepOrder = Optional.ofNullable(existingDetailImageIds).orElse(List.of());
        Set<Long> keepSet = new HashSet<>(keepOrder);

        deleteUnusedImages(current, keepSet);
        product.getImages().clear();

        int order = 0;
        addExistingImagesInOrder(product, byId, keepOrder, order);
        addNewImages(product, newFiles, order);
    }

    private void updateDetailImagesWithOrder(Product product, List<ProductImage> current, 
                                            Map<Long, ProductImage> byId, List<MultipartFile> newFiles, 
                                            List<String> detailImageOrder) {
        Set<Long> referencedIds = extractReferencedIds(detailImageOrder);
        deleteUnusedImages(current, referencedIds);
        product.getImages().clear();

        boolean[] newUsed = new boolean[newFiles.size()];
        int order = 0;

        for (String token : detailImageOrder) {
            if (token.startsWith(ID_PREFIX)) {
                addExistingImageFromToken(product, byId, token, order++);
            } else if (token.startsWith(NEW_PREFIX)) {
                addNewImageFromToken(product, newFiles, token, newUsed, order++);
            } else {
                throw new ProductException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
        }
    }

    private Set<Long> extractReferencedIds(List<String> detailImageOrder) {
        return detailImageOrder.stream()
                .filter(token -> token.startsWith(ID_PREFIX))
                .map(token -> Long.parseLong(token.substring(ID_PREFIX.length())))
                .collect(Collectors.toSet());
    }

    private void deleteUnusedImages(List<ProductImage> current, Set<Long> keepIds) {
        for (ProductImage img : current) {
            if (!keepIds.contains(img.getId())) {
                s3Uploader.deleteFile(img.getImageUrl());
                productImageRepository.delete(img);
            }
        }
    }

    private void addExistingImagesInOrder(Product product, Map<Long, ProductImage> byId, 
                                         List<Long> keepOrder, int startOrder) {
        int order = startOrder;
        for (Long id : keepOrder) {
            ProductImage exist = byId.get(id);
            if (exist != null) {
                product.getImages().add(ProductImage.builder()
                        .product(product)
                        .imageUrl(exist.getImageUrl())
                        .sortOrder(order++)
                        .build());
            }
        }
    }

    private void addNewImages(Product product, List<MultipartFile> newFiles, int startOrder) {
        int order = startOrder;
        for (MultipartFile file : newFiles) {
            String url = s3Uploader.upload(file, DETAIL_PATH);
            product.getImages().add(ProductImage.builder()
                    .product(product)
                    .imageUrl(url)
                    .sortOrder(order++)
                    .build());
        }
    }

    private void addExistingImageFromToken(Product product, Map<Long, ProductImage> byId, 
                                          String token, int order) {
        Long id = Long.parseLong(token.substring(ID_PREFIX.length()));
        ProductImage exist = byId.get(id);
        if (exist != null) {
            product.getImages().add(ProductImage.builder()
                    .product(product)
                    .imageUrl(exist.getImageUrl())
                    .sortOrder(order)
                    .build());
        }
    }

    private void addNewImageFromToken(Product product, List<MultipartFile> newFiles, 
                                     String token, boolean[] newUsed, int order) {
        int idx = Integer.parseInt(token.substring(NEW_PREFIX.length()));
        if (idx < 0 || idx >= newFiles.size() || newUsed[idx]) {
            throw new ProductException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        newUsed[idx] = true;
        MultipartFile file = newFiles.get(idx);
        if (file == null || file.isEmpty()) {
            throw new ProductException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        String url = s3Uploader.upload(file, DETAIL_PATH);
        product.getImages().add(ProductImage.builder()
                .product(product)
                .imageUrl(url)
                .sortOrder(order)
                .build());
    }

    private void updateProductData(Product product, AdminProductUpdateRequest request) {
        productHashTagRepository.deleteAllByProduct(product);
        product.getHashtags().clear();
        product.getHashtags().addAll(request.toHashTags(product));

        productOptionRepository.deleteAllByProduct(product);
        product.getProductOptions().clear();
        product.getProductOptions().addAll(request.toProductOptions(product));
    }
}
