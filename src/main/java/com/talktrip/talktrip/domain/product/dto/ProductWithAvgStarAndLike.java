package com.talktrip.talktrip.domain.product.dto;

import com.talktrip.talktrip.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductWithAvgStarAndLike {
    private Product product;
    private Double avgStar;
    private Boolean isLiked;
}
