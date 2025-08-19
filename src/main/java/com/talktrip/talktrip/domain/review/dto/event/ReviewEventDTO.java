package com.talktrip.talktrip.domain.review.dto.event;

public record ReviewEventDTO(
        Long reviewId,
        Long productId,
        String comment
) {
}
