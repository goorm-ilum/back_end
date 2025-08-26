package com.talktrip.talktrip.domain.product.dto.request;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.global.entity.Country;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record AdminProductCreateRequest(
        @NotBlank(message = "상품명은 필수입니다")
        @Size(min = 1, max = 100, message = "상품명은 1자 이상 100자 이하여야 합니다")
        String productName,
        
        @NotBlank(message = "상품 설명은 필수입니다")
        @Size(min = 10, max = 1000, message = "상품 설명은 10자 이상 1000자 이하여야 합니다")
        String description,
        
        @NotBlank(message = "국가명은 필수입니다")
        String countryName,
        
        @NotEmpty(message = "상품 옵션은 최소 1개 이상 필요합니다")
        @Valid
        List<ProductOptionRequest> options,
        
        List<@Size(min = 1, max = 20, message = "해시태그는 1자 이상 20자 이하여야 합니다") String> hashtags
) {
    public Product to(Member member, Country country) {
        return Product.builder()
                .productName(productName)
                .description(description)
                .member(member)
                .country(country)
                .build();
    }

    public List<HashTag> toHashTags(Product product) {
        return hashtags.stream()
                .map(tag -> HashTag.builder()
                        .product(product)
                        .hashtag(tag)
                        .build())
                .toList();
    }

    public List<ProductOption> toProductOptions(Product product) {
        return options.stream()
                .map(opt -> ProductOption.builder()
                        .product(product)
                        .startDate(opt.startDate())
                        .optionName(opt.optionName())
                        .stock(opt.stock())
                        .price(opt.price())
                        .discountPrice(opt.discountPrice())
                        .build())
                .toList();
    }
}



