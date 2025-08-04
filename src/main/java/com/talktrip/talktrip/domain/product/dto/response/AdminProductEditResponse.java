package com.talktrip.talktrip.domain.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record AdminProductEditResponse(
        String productName,
        String continent,
        String country,
        String thumbnailImageUrl,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate earliestDate,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate latestDate,
        List<OptionStock> optionStocks,
        List<String> images,
        List<String> hashtags
) {
    public record OptionStock(String optionName, int stock, int price, int discountPrice) {}

    public static AdminProductEditResponse from(Product product) {
        List<ProductOption> stocks = product.getProductOptions();
        List<ProductImage> productImages = product.getImages();

        LocalDate earliest = stocks.stream()
                .map(ProductOption::getStartDate)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate latest = stocks.stream()
                .map(ProductOption::getStartDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        List<OptionStock> options = stocks.stream()
                .map(s -> new OptionStock(s.getOptionName(), s.getStock(), s.getPrice(), s.getDiscountPrice()))
                .distinct()
                .toList();

        return AdminProductEditResponse.builder()
                .productName(product.getProductName())
                .continent(product.getCountry().getContinent())
                .country(product.getCountry().getName())
                .thumbnailImageUrl(product.getThumbnailImageUrl())
                .earliestDate(earliest)
                .latestDate(latest)
                .optionStocks(options)
                .images(productImages.stream().map(ProductImage::getImageUrl).toList())
                .hashtags(product.getHashtags().stream().map(HashTag::getHashtag).toList())
                .build();
    }
}
