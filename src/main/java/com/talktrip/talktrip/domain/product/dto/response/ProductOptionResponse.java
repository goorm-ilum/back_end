package com.talktrip.talktrip.domain.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ProductOptionResponse(
        Long productOptionId,
        String optionName,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
        int stock,
        int price,
        int discountPrice
) {
    public static ProductOptionResponse from(ProductOption stock) {
        return ProductOptionResponse.builder()
                .productOptionId(stock.getId())
                .optionName(stock.getOptionName())
                .startDate(stock.getStartDate())
                .stock(stock.getStock())
                .price(stock.getPrice())
                .discountPrice(stock.getDiscountPrice())
                .build();
    }
}