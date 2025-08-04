package com.talktrip.talktrip.domain.product.dto.response;

import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductStock;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminProductSummaryResponse(
        Long id,
        String productName,
        String thumbnailImageUrl,
        int price,
        int discountPrice,
        int totalStock,
        LocalDateTime updatedAt
) {
    public static AdminProductSummaryResponse from(Product product) {
        int stockSum = product.getProductStocks().stream()
                .mapToInt(ProductStock::getStock)
                .sum();

        ProductStock minPriceStock = product.getMinPriceStock();

        int price = minPriceStock != null ? minPriceStock.getPrice() : 0;
        int discountPrice = minPriceStock != null ? minPriceStock.getDiscountPrice() : 0;

        return AdminProductSummaryResponse.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .thumbnailImageUrl(product.getThumbnailImageUrl())
                .price(price)
                .discountPrice(discountPrice)
                .totalStock(stockSum)
                .updatedAt(product.getCreatedAt())
                .build();
    }
}
