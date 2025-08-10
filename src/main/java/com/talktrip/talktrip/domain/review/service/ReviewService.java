package com.talktrip.talktrip.domain.review.service;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.entity.OrderItem;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.order.repository.OrderRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void createReview(Long orderId, Long memberId, ReviewRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ReviewException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getMember().getId().equals(memberId)) throw new ReviewException(ErrorCode.ACCESS_DENIED);
        if (!(order.getOrderStatus() == OrderStatus.SUCCESS))
            throw new ReviewException(ErrorCode.ORDER_NOT_COMPLETED);
        if (reviewRepository.existsByOrderId(orderId)) throw new ReviewException(ErrorCode.ALREADY_REVIEWED);

        Long productId = order.getOrderItems().stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new ReviewException(ErrorCode.ORDER_EMPTY));

        // 소프트 삭제 포함으로 로드해서 Review.product에 세팅
        Product product = productRepository.findByIdIncludingDeleted(productId)
                .orElseThrow(() -> new ReviewException(ErrorCode.PRODUCT_NOT_FOUND));

        Review review = Review.builder()
                .order(order)
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
        if (!review.getMember().getId().equals(memberId)) throw new ReviewException(ErrorCode.ACCESS_DENIED);
        review.update(request.comment(), request.reviewStar());
    }

    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.getMember().getId().equals(memberId)) throw new ReviewException(ErrorCode.ACCESS_DENIED);
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(Long memberId, Pageable pageable) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));

        Page<Review> page = reviewRepository.findByMemberId(memberId, pageable);

        return page.map(r -> {
            // @Where(deleted=false)가 걸린 엔티티 연관 때문에 소프트삭제 상품은 바로 못 불러올 수 있으니,
            // 항상 "삭제 포함"으로 안전하게 로드
            Product product = productRepository.findByIdIncludingDeleted(r.getProduct().getId()).orElse(null);
            return ReviewResponse.from(r, product);
        });
    }

    @Transactional(readOnly = true)
    public MyReviewFormResponse getReviewCreateForm(Long orderId, Long memberId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ReviewException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getMember().getId().equals(memberId)) throw new ReviewException(ErrorCode.ACCESS_DENIED);
        if (!(order.getOrderStatus() == OrderStatus.SUCCESS))
            throw new ReviewException(ErrorCode.ORDER_NOT_COMPLETED);
        if (reviewRepository.existsByOrderId(orderId)) throw new ReviewException(ErrorCode.ALREADY_REVIEWED);

        Long productId = order.getOrderItems().stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        Product product = (productId == null)
                ? null
                : productRepository.findByIdIncludingDeleted(productId).orElse(null);

        return MyReviewFormResponse.from(product, null);
    }

    @Transactional(readOnly = true)
    public MyReviewFormResponse getReviewUpdateForm(Long reviewId, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.getMember().getId().equals(memberId)) throw new ReviewException(ErrorCode.ACCESS_DENIED);

        Product product = productRepository.findByIdIncludingDeleted(review.getProduct().getId())
                .orElse(null);

        return MyReviewFormResponse.from(product, review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsForSellerProduct(Long sellerId, Long productId, Pageable pageable) {
        // 소유권/존재 확인(삭제 포함)
        productRepository.findByIdAndMemberIdIncludingDeleted(productId, sellerId)
                .orElseThrow(() -> new ReviewException(ErrorCode.ACCESS_DENIED));

        Page<Review> page = reviewRepository.findByProductId(productId, pageable);
        Product product = productRepository.findByIdIncludingDeleted(productId).orElse(null);

        return page.map(r -> ReviewResponse.from(r, product));
    }
}
