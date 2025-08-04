package com.talktrip.talktrip.domain.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talktrip.talktrip.domain.product.entity.ProductOption;

import java.time.LocalDate;

public record ProductOptionResponse(
        String optionName,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
        int stock,
        int price,
        int discountPrice
) {
    public static ProductOptionResponse from(ProductOption stock) {
        return new ProductOptionResponse(
                stock.getOptionName(),
                stock.getStartDate(),
                stock.getStock(),
                stock.getPrice(),
                stock.getDiscountPrice()
        );
    }
}