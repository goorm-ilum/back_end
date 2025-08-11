package com.talktrip.talktrip.domain.review.dto.response;

import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.review.entity.Review;

public record ReviewResponse(
        Long reviewId,
        String nickName,
        String productName,
        String thumbnailImageUrl,
        String comment,
        float reviewStar,
        String updatedAt
) {
    public static ReviewResponse from(Review review, Product product) {
        String name = (product != null) ? product.getProductName() : "(삭제된 상품)";
        String thumb = (product != null) ? product.getThumbnailImageUrl() : null;

        return new ReviewResponse(
                review.getId(),
                review.getMember().getNickname(),
                name,
                thumb,
                review.getComment(),
                review.getReviewStar(),
                review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : null
        );
    }
}
