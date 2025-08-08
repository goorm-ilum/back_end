package com.talktrip.talktrip.domain.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminProductSummaryResponse(
        Long id,
        String productName,
        String thumbnailImageUrl,
        int totalStock,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {
    public static AdminProductSummaryResponse from(Product product) {
        int stockSum = product.getProductOptions().stream()
                .mapToInt(ProductOption::getStock)
                .sum();

        return AdminProductSummaryResponse.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .thumbnailImageUrl(product.getThumbnailImageUrl())
                .totalStock(stockSum)
                .updatedAt(product.getCreatedAt())
                .build();
    }
}
