package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductCreateRequest;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductUpdateRequest;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductEditResponse;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.repository.ProductHashTagRepository;
import com.talktrip.talktrip.domain.product.repository.ProductImageRepository;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.product.repository.ProductOptionRepository;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.repository.CountryRepository;
import com.talktrip.talktrip.global.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
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

        // 썸네일 이미지 업로드 및 해시 생성
        String thumbnailUrl = s3Uploader.upload(thumbnailImage, "products/thumbnail");
        String thumbnailHash = s3Uploader.calculateHash(thumbnailImage);

        // 기본 정보 업데이트 (썸네일 해시 포함)
        product.updateBasicInfo(
                request.productName(),
                request.description(),
                country
        );
        product.updateThumbnailImage(thumbnailUrl, thumbnailHash);

        // 상세 이미지 처리
        if (detailImages != null && !detailImages.isEmpty()) {
            List<ProductImage> productImages = detailImages.stream()
                    .map(file -> ProductImage.builder()
                            .product(product)
                            .imageUrl(s3Uploader.upload(file, "products/detail"))
                            .build())
                    .toList();
            product.getImages().addAll(productImages);
        }

        // 해시태그 & 옵션 등록
        product.getHashtags().addAll(request.toHashTags(product));
        product.getProductOptions().addAll(request.toProductOptions(product));

        productRepository.save(product);
    }


    @Transactional(readOnly = true)
    public List<AdminProductSummaryResponse> getMyProducts(Long memberId, int page, int size) {
        int offset = page * size;
        List<Product> products = productRepository.findByMemberId(memberId, offset, size);

        return products.stream()
                .map(AdminProductSummaryResponse::from)
                .toList();
    }


    @Transactional
    public AdminProductEditResponse getMyProductEditForm(Long productId, Long memberId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getMember().getId().equals(memberId)) {
            throw new ProductException(ErrorCode.ACCESS_DENIED);
        }

        return AdminProductEditResponse.from(product);
    }

    @Transactional
    public void updateProduct(Long productId,
                              AdminProductUpdateRequest request,
                              Long memberId,
                              MultipartFile thumbnailImage,
                              List<MultipartFile> detailImages) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getMember().getId().equals(memberId)) {
            throw new ProductException(ErrorCode.ACCESS_DENIED);
        }

        Country country = countryRepository.findByName(request.countryName())
                .orElseThrow(() -> new ProductException(ErrorCode.COUNTRY_NOT_FOUND));

        boolean thumbnailDeleted = (thumbnailImage == null && request.existingThumbnailHash() == null);
        if (thumbnailDeleted) {
            if (product.getThumbnailImageUrl() != null) {
                s3Uploader.deleteFile(product.getThumbnailImageUrl());
            }
            product.updateThumbnailImage(null, null);
        } else if (thumbnailImage != null && !thumbnailImage.isEmpty()) {
            String newThumbHash = s3Uploader.calculateHash(thumbnailImage);
            if (!newThumbHash.equals(product.getThumbnailImageHash())) {
                if (product.getThumbnailImageUrl() != null) {
                    s3Uploader.deleteFile(product.getThumbnailImageUrl());
                }
                String uploadedUrl = s3Uploader.upload(thumbnailImage, "products/thumbnail");
                product.updateThumbnailImage(uploadedUrl, newThumbHash);
            }
        }

        List<ProductImage> currentImages = productImageRepository.findAllByProduct(product);
        Set<Long> keepIds = new HashSet<>(Optional.ofNullable(request.existingDetailImageIds()).orElse(List.of()));

        List<ProductImage> imagesToDelete = currentImages.stream()
                .filter(img -> !keepIds.contains(img.getId()))
                .toList();

        for (ProductImage img : imagesToDelete) {
            s3Uploader.deleteFile(img.getImageUrl());
        }
        productImageRepository.deleteAll(imagesToDelete);
        product.getImages().removeAll(imagesToDelete);

        if (detailImages != null) {
            for (MultipartFile file : detailImages) {
                String url = s3Uploader.upload(file, "products/detail");
                product.getImages().add(
                        ProductImage.builder()
                                .product(product)
                                .imageUrl(url)
                                .build()
                );
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
        Product product = productRepository.findByIdAndMemberId(productId, memberId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        s3Uploader.deleteFile(product.getThumbnailImageUrl());

        List<ProductImage> detailImages = productImageRepository.findAllByProduct(product);
        for (ProductImage image : detailImages) {
            s3Uploader.deleteFile(image.getImageUrl());
        }

        productImageRepository.deleteAllByProduct(product);
        productHashTagRepository.deleteAllByProduct(product);
        productOptionRepository.deleteAllByProduct(product);

        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<AdminProductSummaryResponse> searchMyProducts(Long memberId, String keyword, int page, int size, String sortBy, boolean ascending) {
        List<Product> products;

        if (keyword == null || keyword.trim().isEmpty()) {
            products = productRepository.findByMemberId(memberId);
        } else {
            List<String> keywords = Arrays.stream(keyword.trim().split("\\s+")).toList();
            List<Product> candidates = productRepository.searchByKeywordsAndMemberId(keywords, memberId);

            products = candidates.stream()
                    .filter(product -> {
                        String combined = (product.getProductName() + " " + product.getDescription()).toLowerCase();
                        List<String> combinedWords = Arrays.asList(combined.split("\\s+"));

                        for (String k : keywords) {
                            long count = combinedWords.stream().filter(word -> word.equals(k.toLowerCase())).count();
                            long required = keywords.stream().filter(s -> s.equalsIgnoreCase(k)).count();
                            if (count < required) return false;
                        }
                        return true;
                    })
                    .sorted(getComparator(sortBy, ascending))
                    .toList();
        }

        int offset = page * size;
        int toIndex = Math.min(offset + size, products.size());

        List<Product> pagedProducts = (offset > products.size()) ? List.of() : products.subList(offset, toIndex);

        return pagedProducts.stream()
                .map(AdminProductSummaryResponse::from)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<AdminProductSummaryResponse> sortMyProducts(Long memberId, int page, int size, String sortBy, boolean ascending) {
        List<Product> products = productRepository.findByMemberId(memberId);

        products.sort(getComparator(sortBy, ascending));

        int offset = page * size;
        int toIndex = Math.min(offset + size, products.size());

        List<Product> pagedProducts = (offset > products.size()) ? List.of() : products.subList(offset, toIndex);

        return pagedProducts.stream()
                .map(AdminProductSummaryResponse::from)
                .toList();
    }


    private Comparator<Product> getComparator(String sortBy, boolean ascending) {
        Comparator<Product> comparator;
        switch (sortBy) {
            case "productName" -> comparator = Comparator.comparing(Product::getProductName, String.CASE_INSENSITIVE_ORDER);
            case "price" -> comparator = Comparator.comparing(p -> Optional.ofNullable(p.getMinPriceOption()).map(ProductOption::getPrice).orElse(0));
            case "discountPrice" -> comparator = Comparator.comparing(p -> Optional.ofNullable(p.getMinPriceOption()).map(ProductOption::getDiscountPrice).orElse(0));
            case "totalStock" -> comparator = Comparator.comparing(Product::getTotalStock);
            case "updatedAt" -> comparator = Comparator.comparing(Product::getUpdatedAt);
            default -> throw new IllegalArgumentException("Invalid sortBy value: " + sortBy);
        }
        return ascending ? comparator : comparator.reversed();
    }
}
