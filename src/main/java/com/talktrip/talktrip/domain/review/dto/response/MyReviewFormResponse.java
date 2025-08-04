package com.talktrip.talktrip.domain.review.dto.response;

import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.review.entity.Review;

public record MyReviewFormResponse(
        String productName,
        String thumbnailUrl,
        Float myStar,
        String myComment
) {
    public static MyReviewFormResponse from(Product product, Review review) {
        return new MyReviewFormResponse(
                product.getProductName(),
                product.getThumbnailImageUrl(),
                review.getReviewStar(),
                review.getComment()
        );
    }
}
