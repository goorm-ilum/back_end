package com.talktrip.talktrip.domain.review.service;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.dto.request.ReviewRequest;
import com.talktrip.talktrip.domain.review.dto.response.MyReviewFormResponse;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ReviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void createReview(Long productId, Long memberId, ReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ReviewException(ErrorCode.PRODUCT_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));

        Review review = Review.builder()
                .product(product)
                .member(member)
                .comment(request.comment())
                .reviewStar(request.reviewStar())
                .build();

        reviewRepository.save(review);
    }

    @Transactional
    public void updateReview(Long reviewId, Long memberId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getMember().getId().equals(memberId)) {
            throw new ReviewException(ErrorCode.ACCESS_DENIED);
        }

        review.update(request.comment(), request.reviewStar());
    }

    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getMember().getId().equals(memberId)) {
            throw new ReviewException(ErrorCode.ACCESS_DENIED);
        }

        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));

        return reviewRepository.findByMemberOrderByCreatedAtDesc(member).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MyReviewFormResponse getReviewCreateForm(Long productId, Long memberId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ReviewException(ErrorCode.PRODUCT_NOT_FOUND));

        Optional<Review> reviewOpt = reviewRepository.findByProductIdAndMemberId(productId, memberId);
        if (reviewOpt.isPresent()) {
            throw new ReviewException(ErrorCode.ALREADY_REVIEWED);
        }

        return MyReviewFormResponse.from(product, null); // 👍 리뷰가 없으므로 null 전달
    }

    @Transactional(readOnly = true)
    public MyReviewFormResponse getReviewUpdateForm(Long reviewId, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getMember().getId().equals(memberId)) {
            throw new ReviewException(ErrorCode.ACCESS_DENIED);
        }

        Product product = review.getProduct();

        return MyReviewFormResponse.from(product, review);
    }


}
