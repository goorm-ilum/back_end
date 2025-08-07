package com.talktrip.talktrip.domain.product.dto.request;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.global.entity.Country;

import java.util.List;

public record AdminProductCreateRequest(
        String productName,
        String description,
        String countryName,
        List<ProductOptionRequest> options,
        List<String> hashtags
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



