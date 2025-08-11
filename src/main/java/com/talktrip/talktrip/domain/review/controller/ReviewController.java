package com.talktrip.talktrip.domain.review.controller;

import com.talktrip.talktrip.domain.review.dto.request.ReviewRequest;
import com.talktrip.talktrip.domain.review.dto.response.MyReviewFormResponse;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;
import com.talktrip.talktrip.domain.review.service.ReviewService;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import com.talktrip.talktrip.global.util.SortUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Review", description = "리뷰 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 작성")
    @PostMapping("/orders/{orderId}/review")
    public ResponseEntity<Void> createReview(
            @PathVariable Long orderId,
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal CustomMemberDetails memberDetails) {

        reviewService.createReview(orderId, memberDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "리뷰 수정")
    @PutMapping("/reviews/{reviewId}")
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
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal CustomMemberDetails memberDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "updatedAt,desc") List<String> sort
    ) {
        Pageable pageable = PageRequest.of(page, size, SortUtil.buildSort(sort));
        Page<ReviewResponse> reviews = reviewService.getMyReviews(memberDetails.getId(), pageable);
        return ResponseEntity.ok(reviews);
    }

    @Operation(summary = "리뷰 작성 폼 (아직 없을 때)")
    @GetMapping("/orders/{orderId}/review/form")
    public ResponseEntity<MyReviewFormResponse> getReviewCreateForm(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomMemberDetails memberDetails) {

        MyReviewFormResponse response = reviewService.getReviewCreateForm(orderId, memberDetails.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "리뷰 수정 폼 (내 리뷰)")
    @GetMapping("/reviews/{reviewId}/form")
    public ResponseEntity<MyReviewFormResponse> getReviewUpdateForm(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomMemberDetails memberDetails) {

        MyReviewFormResponse response = reviewService.getReviewUpdateForm(reviewId, memberDetails.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "특정 상품 리뷰 목록")
    @GetMapping("/admin/products/{productId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getReviewsForSellerProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomMemberDetails memberDetails, // seller
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt,desc") List<String> sort
    ) {
        Pageable pageable = PageRequest.of(page, size, SortUtil.buildSort(sort));
        Page<ReviewResponse> pageResult =
                reviewService.getReviewsForAdminProduct(memberDetails.getId(), productId, pageable);
        return ResponseEntity.ok(pageResult);
    }
}
