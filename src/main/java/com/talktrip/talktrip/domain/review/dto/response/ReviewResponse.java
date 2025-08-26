package com.talktrip.talktrip.domain.review.dto.response;

import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.review.entity.Review;
import lombok.Builder;

import java.util.List;

@Builder
public record ReviewResponse(
        Long reviewId,
        String nickName,
        String productName,
        String thumbnailImageUrl,
        String comment,
        Double reviewStar,
        String updatedAt
) {
    public static ReviewResponse from(Review review, Product product) {
        return new ReviewResponse(
                review.getId(),
                review.getMember().getNickname(),
                getProductName(product),
                getProductThumbnail(product),
                review.getComment(),
                review.getReviewStar(),
                formatUpdatedAt(review.getUpdatedAt())
        );
    }

    private static String getProductName(Product product) {
        return (product != null) ? product.getProductName() : "(삭제된 상품)";
    }

    private static String getProductThumbnail(Product product) {
        return (product != null) ? product.getThumbnailImageUrl() : null;
    }

    private static String formatUpdatedAt(java.time.LocalDateTime updatedAt) {
        return (updatedAt != null) ? updatedAt.toString() : null;
    }

    public static List<ReviewResponse> to(List<Review> reviews, Product product) {
        return reviews.stream()
                .map(review -> ReviewResponse.from(review, product))
                .toList();
    }
}
