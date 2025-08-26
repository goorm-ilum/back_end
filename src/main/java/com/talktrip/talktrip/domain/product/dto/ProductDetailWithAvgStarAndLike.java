package com.talktrip.talktrip.domain.product.dto;

import com.talktrip.talktrip.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailWithAvgStarAndLike {
    private Product product;
    private Double avgStar;
    private Boolean isLiked;
}
