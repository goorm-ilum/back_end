package com.talktrip.talktrip.domain.product.dto.response;

import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;

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
        ProductOption minPriceStock = product.getMinPriceOption();

        int price = minPriceStock != null ? minPriceStock.getPrice() : 0;
        int discountPrice = minPriceStock != null ? minPriceStock.getDiscountPrice() : 0;

        return new ProductSummaryResponse(
                product.getId(),
                product.getProductName(),
                product.getDescription(),
                product.getThumbnailImageUrl(),
                price,
                discountPrice,
                avgStar,
                isLiked
        );
    }
}
