package com.talktrip.talktrip.domain.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Review", description = "리뷰 관련 API")
@RestController
@RequestMapping("/api")
public class ReviewController {

    @Operation(summary = "리뷰 작성")
    @PostMapping("/products/{productId}/reviews")
    public void writeReview(@PathVariable Long productId) {
    }

    @Operation(summary = "내 리뷰 수정")
    @PatchMapping("/reviews/{reviewId}")
    public void updateReview(@PathVariable Long reviewId) {
    }

    @Operation(summary = "내 리뷰 삭제")
    @DeleteMapping("/reviews/{reviewId}")
    public void deleteReview(@PathVariable Long reviewId) {
    }
}
