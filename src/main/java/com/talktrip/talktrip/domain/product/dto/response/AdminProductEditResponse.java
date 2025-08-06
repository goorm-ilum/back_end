package com.talktrip.talktrip.domain.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public record AdminProductEditResponse(
        String productName,
        String description,
        String continent,
        String country,
        String thumbnailImageUrl,
        String thumbnailImageHash,
        @JsonFormat(pattern = "yyyy-MM-dd")
        List<LocalDate> startDates,
        List<OptionStock> optionStocks,
        List<ImageInfo> images,
        List<String> hashtags
) {
    public record OptionStock(String optionName, int stock, int price, int discountPrice) {}

    public record ImageInfo(Long imageId, String imageUrl) {}

    public static AdminProductEditResponse from(Product product) {
        List<ProductOption> stocks = product.getProductOptions();
        List<ProductImage> productImages = product.getImages();

        List<LocalDate> startDates = stocks.stream()
                .map(ProductOption::getStartDate)
                .distinct()
                .sorted()
                .toList();

        List<OptionStock> options = stocks.stream()
                .map(s -> new OptionStock(s.getOptionName(), s.getStock(), s.getPrice(), s.getDiscountPrice()))
                .distinct()
                .toList();

        List<ImageInfo> imageInfos = productImages.stream()
                .map(img -> new ImageInfo(img.getId(), img.getImageUrl()))
                .toList();

        return AdminProductEditResponse.builder()
                .productName(product.getProductName())
                .description(product.getDescription())
                .continent(product.getCountry().getContinent())
                .country(product.getCountry().getName())
                .thumbnailImageUrl(product.getThumbnailImageUrl())
                .thumbnailImageHash(product.getThumbnailImageHash())
                .startDates(startDates)
                .optionStocks(options)
                .images(imageInfos)
                .hashtags(product.getHashtags().stream().map(HashTag::getHashtag).toList())
                .build();
    }
}
