package com.talktrip.talktrip.domain.product.dto.request;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.entity.ProductStock;
import com.talktrip.talktrip.global.entity.Country;

import java.time.LocalDate;
import java.util.List;

public record AdminProductCreateRequest(
        String productName,
        String description,
        int price,
        int discountPrice,
        String thumbnailImageUrl,
        String countryName,
        List<LocalDate> startDates,
        List<OptionStockRequest> optionStocks,
        List<String> hashtags,
        List<String> imageUrls
) {
    public record OptionStockRequest(String option, int stock) {}

    public Product to(Member member, Country country) {
        return Product.builder()
                .productName(productName)
                .description(description)
                .price(price)
                .discountPrice(discountPrice)
                .thumbnailImageUrl(thumbnailImageUrl)
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
                                .option(os.option())
                                .stock(os.stock())
                                .build()))
                .toList();
    }

    public List<ProductImage> toProductImages(Product product) {
        if (imageUrls == null) return List.of();

        return imageUrls.stream()
                .map(url -> ProductImage.builder()
                        .product(product)
                        .imageUrl(url)
                        .build())
                .toList();
    }
}


