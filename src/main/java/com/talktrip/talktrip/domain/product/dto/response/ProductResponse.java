package com.talktrip.talktrip.domain.product.dto.response;

import com.talktrip.talktrip.domain.product.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private int price;
    private int stock;
    private LocalDateTime departureDate;
    private LocalDateTime arrivalDate;
    private Product.ProductType type;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .departureDate(product.getDepartureDate())
                .arrivalDate(product.getArrivalDate())
                .type(product.getType())
                .build();
    }
}
