package com.talktrip.talktrip.domain.review.dto.response;

import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyReviewFormResponse {

    private Long reviewId;
    private String productName;
    private String thumbnailUrl;
    private Float myStar;
    private String myComment;

    public static MyReviewFormResponse from(Product product, Review review) {
        String name = (product != null) ? product.getProductName() : "(삭제된 상품)";
        String thumb = (product != null) ? product.getThumbnailImageUrl() : null;

        return MyReviewFormResponse.builder()
                .reviewId(review != null ? review.getId() : null)
                .productName(name)
                .thumbnailUrl(thumb)
                .myStar(review != null ? review.getReviewStar() : null)
                .myComment(review != null ? review.getComment() : null)
                .build();
    }
}
