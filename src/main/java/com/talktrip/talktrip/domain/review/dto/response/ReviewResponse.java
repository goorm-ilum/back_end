package com.talktrip.talktrip.domain.review.dto.response;

import com.talktrip.talktrip.domain.review.entity.Review;

public record ReviewResponse(
        Long reviewId,
        String comment,
        float reviewStar,
        String reviewerNickname,
        String updatedAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getComment(),
                review.getReviewStar(),
                review.getReviewer().getNickname(),
                review.getUpdatedAt().toString()
        );
    }
}
