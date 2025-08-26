package com.talktrip.talktrip.domain.like.dto;

import com.talktrip.talktrip.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductWithAvgStar {
    private Product product;
    private Double avgStar;
}
