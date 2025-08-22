package com.talktrip.talktrip.domain.product.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ProductSummaryResponse(
        Long productId,
        String productName,
        String productDescription,
        String thumbnailImageUrl,
        int minPrice,
        int minDiscountPrice,
        float averageReviewStar,
        boolean isLiked,
        LocalDateTime likedAt
) {

    public ProductSummaryResponse(Long productId, String productName, String productDescription, 
                                 String thumbnailImageUrl, int minPrice, int minDiscountPrice,
                                 float averageReviewStar, boolean isLiked) {
        this(productId, productName, productDescription, thumbnailImageUrl, minPrice, minDiscountPrice, averageReviewStar, isLiked, null);
    }

    public static ProductSummaryResponse from(ProductSummaryResponse response, boolean isLiked) {
        return ProductSummaryResponse.builder()
                .productId(response.productId())
                .productName(response.productName())
                .productDescription(response.productDescription())
                .thumbnailImageUrl(response.thumbnailImageUrl())
                .minPrice(response.minPrice())
                .minDiscountPrice(response.minDiscountPrice())
                .averageReviewStar(response.averageReviewStar())
                .isLiked(isLiked)
                .likedAt(response.likedAt())
                .build();
    }
}
