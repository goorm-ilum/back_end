package com.talktrip.talktrip.domain.product.dto.request;

import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;

import java.util.List;

public record AdminProductUpdateRequest(
        String productName,
        String description,
        String countryName,
        List<ProductOptionRequest> options,
        List<String> hashtags,
        String existingThumbnailHash,
        List<Long> existingDetailImageIds
) {
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

