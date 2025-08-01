package com.talktrip.talktrip.domain.product.dto.response;

import com.talktrip.talktrip.domain.product.entity.Product;

public record ProductSummaryResponse(
        Long productId,
        String productName,
        String productDescription,
        String thumbnailImageUrl,
        int price,
        int discountPrice,
        float averageReviewStar,
        boolean isLiked
) {
    public static ProductSummaryResponse from(Product product, float avgStar, boolean isLiked) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getProductName(),
                product.getDescription(),
                product.getThumbnailImageUrl(),
                product.getPrice(),
                product.getDiscountPrice(),
                avgStar,
                isLiked
        );
    }
}
