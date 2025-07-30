package com.talktrip.talktrip.domain.product.dto.response;

import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.entity.ProductStock;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record AdminProductEditResponse(
        String productName,
        String continent,
        String country,
        String thumbnailImageUrl,
        int price,
        int discountPrice,
        LocalDate earliestDate,
        LocalDate latestDate,
        List<OptionStock> optionStocks,
        List<String> images,
        List<String> hashtags
) {
    public record OptionStock(String option, int stock) {}

    public static AdminProductEditResponse from(Product product) {
        List<ProductStock> stocks = product.getProductStocks();
        List<ProductImage> productImages = product.getImages();

        LocalDate earliest = stocks.stream()
                .map(ProductStock::getStartDate)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate latest = stocks.stream()
                .map(ProductStock::getStartDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        List<OptionStock> options = stocks.stream()
                .map(s -> new OptionStock(s.getOption(), s.getStock()))
                .distinct()
                .toList();

        return AdminProductEditResponse.builder()
                .productName(product.getProductName())
                .continent(product.getCountry().getContinent())
                .country(product.getCountry().getName())
                .thumbnailImageUrl(product.getThumbnailImageUrl())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .earliestDate(earliest)
                .latestDate(latest)
                .optionStocks(options)
                .images(productImages.stream().map(ProductImage::getImageUrl).toList())
                .hashtags(product.getHashtags().stream().map(HashTag::getHashtag).toList())
                .build();
    }
}
