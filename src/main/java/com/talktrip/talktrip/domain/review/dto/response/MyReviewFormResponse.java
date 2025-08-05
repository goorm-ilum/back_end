package com.talktrip.talktrip.domain.review.dto.response;

import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.review.entity.Review;

public record MyReviewFormResponse(
        Long reviewId,
        String productName,
        String thumbnailUrl,
        Float myStar,
        String myComment
) {
    public static MyReviewFormResponse from(Product product, Review review) {
        return new MyReviewFormResponse(
                review != null ? review.getId() : null,
                product.getProductName(),
                product.getThumbnailImageUrl(),
                review != null ? review.getReviewStar() : null,
                review != null ? review.getComment() : null
        );
    }

}
