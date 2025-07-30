package com.talktrip.talktrip.domain.product.dto.response;

import com.talktrip.talktrip.domain.product.entity.ProductStock;

import java.time.LocalDate;

public record ProductStockResponse(
        String option,
        LocalDate startDate,
        int stock
) {
    public static ProductStockResponse from(ProductStock stock) {
        return new ProductStockResponse(
                stock.getOption(),
                stock.getStartDate(),
                stock.getStock()
        );
    }
}
