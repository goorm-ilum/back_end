package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductCreateRequest;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductEditResponse;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

        String thumbnailUrl = s3Uploader.upload(thumbnailImage, "products/thumbnail");
        product.updateBasicInfo(
                request.productName(),
                request.description(),
                thumbnailUrl,
                country
        );

        List<ProductImage> productImages = detailImages.stream()
                .map(file -> ProductImage.builder()
                        .product(product)
                        .imageUrl(s3Uploader.upload(file, "products/detail"))
                        .build())
                .toList();
        product.getImages().addAll(productImages);

        product.getHashtags().addAll(request.toHashTags(product));
        product.getProductOptions().addAll(request.toProductOptions(product));

        productRepository.save(product);
    }

    @Transactional
    public List<AdminProductSummaryResponse> getMyProducts(Long memberId) {
        List<Product> products = productRepository.findByMemberId(memberId);
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
    public void updateProduct(Long productId, AdminProductCreateRequest request, Long memberId,
                              MultipartFile thumbnailImage, List<MultipartFile> detailImages) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.getMember().getId().equals(memberId)) {
            throw new ProductException(ErrorCode.ACCESS_DENIED);
        }

        Country country = countryRepository.findByName(request.countryName())
                .orElseThrow(() -> new ProductException(ErrorCode.COUNTRY_NOT_FOUND));

        if (product.getThumbnailImageUrl() != null) {
            s3Uploader.delete(product.getThumbnailImageUrl());
        }

        String newThumbnailUrl = s3Uploader.upload(thumbnailImage, "products/thumbnail");
        product.updateBasicInfo(
                request.productName(),
                request.description(),
                newThumbnailUrl,
                country
        );

        product.getImages().forEach(image -> s3Uploader.delete(image.getImageUrl()));
        product.getImages().clear();

        List<ProductImage> newImages = detailImages.stream()
                .map(file -> ProductImage.builder()
                        .product(product)
                        .imageUrl(s3Uploader.upload(file, "products/detail"))
                        .build())
                .toList();
        product.getImages().addAll(newImages);
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
}
