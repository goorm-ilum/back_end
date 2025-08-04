package com.talktrip.talktrip.domain.product.dto.request;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductStock;
import com.talktrip.talktrip.global.entity.Country;

import java.time.LocalDate;
import java.util.List;

public record AdminProductCreateRequest(
        String productName,
        String description,
        int price,
        int discountPrice,
        String countryName,
        List<LocalDate> startDates,
        List<OptionStockRequest> optionStocks,
        List<String> hashtags
) {
    public record OptionStockRequest(String optionName, int stock, int price, int discountPrice) {}

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

    public List<ProductStock> toProductStocks(Product product) {
        return startDates.stream()
                .flatMap(date -> optionStocks.stream()
                        .map(os -> ProductStock.builder()
                                .product(product)
                                .startDate(date)
                                .optionName(os.optionName)
                                .price(os.price)
                                .discountPrice(os.discountPrice)
                                .stock(os.stock())
                                .build()))
                .toList();
    }
}


