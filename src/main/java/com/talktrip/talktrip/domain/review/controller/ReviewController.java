package com.talktrip.talktrip.domain.review.controller;

import com.talktrip.talktrip.domain.review.dto.request.ReviewRequest;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;
import com.talktrip.talktrip.domain.review.service.ReviewService;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Review", description = "리뷰 관련 API")
@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 작성")
    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<Void> createReview(
            @PathVariable Long productId,
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal CustomMemberDetails memberDetails) {

        reviewService.createReview(productId, memberDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "리뷰 수정")
    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal CustomMemberDetails memberDetails) {

        reviewService.updateReview(reviewId, memberDetails.getId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "리뷰 삭제")
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomMemberDetails memberDetails) {

        reviewService.deleteReview(reviewId, memberDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 리뷰 목록 조회")
    @GetMapping("/me/reviews")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal CustomMemberDetails memberDetails) {

        List<ReviewResponse> reviews = reviewService.getMyReviews(memberDetails.getId());
        return ResponseEntity.ok(reviews);
    }

}
