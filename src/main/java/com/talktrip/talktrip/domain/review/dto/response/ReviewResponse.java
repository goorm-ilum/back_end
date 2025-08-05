package com.talktrip.talktrip.domain.review.dto.response;

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
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getMember().getNickname(),
                review.getProduct().getProductName(),
                review.getProduct().getThumbnailImageUrl(),
                review.getComment(),
                review.getReviewStar(),
                review.getUpdatedAt().toString()
        );
    }
}
