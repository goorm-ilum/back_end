package com.talktrip.talktrip.domain.product.dto.response;

import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import lombok.Builder;

import java.util.List;

@Builder
public record AdminProductEditResponse(
        String productName,
        String description,
        String continent,
        String country,
        String thumbnailImageUrl,
        String thumbnailImageHash,
        List<ProductOptionResponse> options,
        List<ImageInfo> images,
        List<String> hashtags
) {
    public record ImageInfo(Long imageId, String imageUrl) {}

    public static AdminProductEditResponse from(Product product) {
        List<ProductOptionResponse> options = product.getProductOptions().stream()
                .map(ProductOptionResponse::from)
                .toList();

        List<ImageInfo> imageInfos = product.getImages().stream()
                .map(img -> new ImageInfo(img.getId(), img.getImageUrl()))
                .toList();

        return AdminProductEditResponse.builder()
                .productName(product.getProductName())
                .description(product.getDescription())
                .continent(product.getCountry().getContinent())
                .country(product.getCountry().getName())
                .thumbnailImageUrl(product.getThumbnailImageUrl())
                .thumbnailImageHash(product.getThumbnailImageHash())
                .options(options)
                .images(imageInfos)
                .hashtags(product.getHashtags().stream().map(HashTag::getHashtag).toList())
                .build();
    }
}
