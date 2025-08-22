package com.talktrip.talktrip.domain.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ProductDetailResponse(
        Long productId,
        String productName,
        String shortDescription,
        int price,
        int discountPrice,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime regDate,
        String thumbnailImageUrl,
        String countryName,
        List<String> hashtags,
        List<String> images,
        List<ProductOptionResponse> stocks,
        float averageReviewStar,
        List<ReviewResponse> reviews,
        boolean isLiked,
        String sellerName,
        String email,
        String phoneNum
) {
    public static ProductDetailResponse from(Product product, float avgStar, List<ReviewResponse> reviews, boolean isLiked) {
        List<ProductOption> futureOptions = getFutureOptions(product);
        ProductOption minPriceStock = product.getMinPriceOption();

        return ProductDetailResponse.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .shortDescription(product.getDescription())
                .price(getPrice(minPriceStock))
                .discountPrice(getDiscountPrice(minPriceStock))
                .regDate(product.getUpdatedAt())
                .thumbnailImageUrl(product.getThumbnailImageUrl())
                .countryName(product.getCountry().getName())
                .hashtags(getHashtagNames(product))
                .images(getImageUrls(product))
                .stocks(futureOptions.stream().map(ProductOptionResponse::from).toList())
                .averageReviewStar(avgStar)
                .reviews(reviews)
                .isLiked(isLiked)
                .sellerName(product.getMember().getName())
                .email(product.getMember().getAccountEmail())
                .phoneNum(product.getMember().getPhoneNum())
                .build();
    }
    
    private static List<ProductOption> getFutureOptions(Product product) {
        return product.getProductOptions().stream()
                .filter(option -> !option.getStartDate().isBefore(LocalDate.now()))
                .toList();
    }

    private static int getPrice(ProductOption minPriceStock) {
        return minPriceStock != null ? minPriceStock.getPrice() : 0;
    }

    private static int getDiscountPrice(ProductOption minPriceStock) {
        return minPriceStock != null ? minPriceStock.getDiscountPrice() : 0;
    }

    private static List<String> getHashtagNames(Product product) {
        return product.getHashtags().stream().map(HashTag::getHashtag).toList();
    }

    private static List<String> getImageUrls(Product product) {
        return product.getImages().stream().map(ProductImage::getImageUrl).toList();
    }
}
