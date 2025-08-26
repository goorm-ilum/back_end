package com.talktrip.talktrip.domain.review.dto.response;

import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.review.entity.Review;
import lombok.Builder;

@Builder
public record MyReviewFormResponse(
        Long reviewId,
        String productName,
        String thumbnailUrl,
        Double myStar,
        String myComment
) {
    public static MyReviewFormResponse from(Product product, Review review) {
        String name = (product != null) ? product.getProductName() : "(삭제된 상품)";
        String thumb = (product != null) ? product.getThumbnailImageUrl() : null;

        return new MyReviewFormResponse(
                review != null ? review.getId() : null,
                name,
                thumb,
                review != null ? review.getReviewStar() : null,
                review != null ? review.getComment() : null
        );
    }
}
