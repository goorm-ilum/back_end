package com.talktrip.talktrip.domain.product.dto.response;

import com.talktrip.talktrip.domain.product.entity.ProductStock;

import java.time.LocalDate;

public record ProductStockResponse(
        String optionName,
        LocalDate startDate,
        int stock,
        int price,
        int discountPrice
) {
    public static ProductStockResponse from(ProductStock stock) {
        return new ProductStockResponse(
                stock.getOptionName(),
                stock.getStartDate(),
                stock.getStock(),
                stock.getPrice(),
                stock.getDiscountPrice()
        );
    }
}
