package com.talktrip.talktrip.domain.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProductDetailResponse(
        Long productId,
        String productName,
        String shortDescription,
        int price,
        int discountPrice,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime regDate,
        String thumbnailImageUrl,
        String countryName,
        List<String> hashtags,
        List<String> images,
        List<ProductOptionResponse> stocks,
        float averageReviewStar,
        List<ReviewResponse> reviews,
        boolean isLiked,
        String sellerName,
        String email,
        String phoneNum
) {
    public static ProductDetailResponse from(Product product, float avgStar, List<ReviewResponse> reviews, boolean isLiked) {
        List<ProductOption> futureOptions = product.getProductOptions().stream()
                .filter(option -> !option.getStartDate().isBefore(LocalDate.now()))
                .toList();

        ProductOption minPriceStock = product.getMinPriceOption();

        int price = minPriceStock != null ? minPriceStock.getPrice() : 0;
        int discountPrice = minPriceStock != null ? minPriceStock.getDiscountPrice() : 0;

        return new ProductDetailResponse(
                product.getId(),
                product.getProductName(),
                product.getDescription(),
                price,
                discountPrice,
                product.getUpdatedAt(),
                product.getThumbnailImageUrl(),
                product.getCountry().getName(),
                product.getHashtags().stream().map(HashTag::getHashtag).toList(),
                product.getImages().stream().map(ProductImage::getImageUrl).toList(),
                futureOptions.stream().map(ProductOptionResponse::from).toList(),
                avgStar,
                reviews,
                isLiked,
                product.getMember().getName(),
                product.getMember().getAccountEmail(),
                product.getMember().getPhoneNum()
        );
    }
}
