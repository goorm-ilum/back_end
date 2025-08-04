package com.talktrip.talktrip.domain.product.dto.response;

import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.entity.ProductStock;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;

import java.time.LocalDateTime;
import java.util.List;

public record ProductDetailResponse(
        Long productId,
        String productName,
        String shortDescription,
        int price,
        int discountPrice,
        LocalDateTime regDate,
        String thumbnailImageUrl,
        String countryName,
        List<String> hashtags,
        List<String> images,
        List<ProductStockResponse> stocks,
        float averageReviewStar,
        List<ReviewResponse> reviews,
        boolean isLiked
) {
    public static ProductDetailResponse from(Product product, float avgStar, List<ReviewResponse> reviews, boolean isLiked) {
        ProductStock minPriceStock = product.getMinPriceStock();

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
                product.getProductStocks().stream().map(ProductStockResponse::from).toList(),
                avgStar,
                reviews,
                isLiked
        );
    }
}
