package com.talktrip.talktrip.domain.product.dto.request;

import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;

import java.time.LocalDate;
import java.util.List;

public record AdminProductUpdateRequest(
        String productName,
        String description,
        String countryName,
        List<LocalDate> startDates,
        List<AdminProductCreateRequest.OptionStockRequest> optionStocks,
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
        return startDates.stream()
                .flatMap(date -> optionStocks.stream()
                        .map(os -> ProductOption.builder()
                                .product(product)
                                .startDate(date)
                                .optionName(os.optionName())
                                .price(os.price())
                                .discountPrice(os.discountPrice())
                                .stock(os.stock())
                                .build()))
                .toList();
    }
}

