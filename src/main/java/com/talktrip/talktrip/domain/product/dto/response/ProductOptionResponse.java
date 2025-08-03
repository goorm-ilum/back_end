package com.talktrip.talktrip.domain.product.dto.response;

import com.talktrip.talktrip.domain.product.entity.ProductOption;

public record ProductOptionResponse(
        Long id,
        String optionName,
        int extraPrice,
        int stock
) {
    public static ProductOptionResponse from(ProductOption productOption) {
        return new ProductOptionResponse(
                productOption.getId(),
                productOption.getOptionName(),
                productOption.getExtraPrice(),
                productOption.getStock()
        );
    }
} 