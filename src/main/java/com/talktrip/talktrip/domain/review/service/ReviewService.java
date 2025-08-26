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
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.OrderException;
import com.talktrip.talktrip.global.exception.ReviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        validateMember(memberId);
        validateOrder(orderId);
        
        Member member = findMember(memberId);
        Order order = findOrder(orderId);
        
        validateOrderAccess(order, memberId);
        validateOrderStatus(order);
        validateReviewNotExists(orderId);

        Long productId = extractProductId(order);
        validateProduct(productId);
        Product product = findProduct(productId);

        Review review = Review.to(order, product, member, request);
        reviewRepository.save(review);
    }

    @Transactional
    public void updateReview(Long reviewId, Long memberId, ReviewRequest request) {
        validateMember(memberId);
        validateReview(reviewId);
        
        Review review = findReview(reviewId);
        validateReviewAccess(review, memberId);
        review.update(request.comment(), request.reviewStar());
    }

    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {
        validateMember(memberId);
        validateReview(reviewId);
        
        Review review = findReview(reviewId);
        validateReviewAccess(review, memberId);
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(Long memberId, Pageable pageable) {
        validateMember(memberId);
        Page<Review> page = reviewRepository.findByMemberIdWithProduct(memberId, pageable);
        return page.map(review -> ReviewResponse.from(review, review.getProduct()));
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsForAdminProduct(
            Long sellerId,
            Long productId,
            Pageable pageable
    ) {
        validateMember(sellerId);
        validateProduct(productId);
        validateProductAccess(productId, sellerId);

        Page<Review> reviewPage = reviewRepository.findByProductIdWithPaging(productId, pageable);
        Product product = findProductOrNull(productId);
        return reviewPage.map(review -> ReviewResponse.from(review, product));
    }

    @Transactional(readOnly = true)
    public MyReviewFormResponse getReviewCreateForm(Long orderId, Long memberId) {
        validateMember(memberId);
        validateOrder(orderId);
        
        Order order = findOrder(orderId);
        validateOrderAccess(order, memberId);
        validateOrderStatus(order);
        validateReviewNotExists(orderId);

        Long productId = extractProductId(order);
        validateProduct(productId);
        Product product = findProductOrNull(productId);

        return MyReviewFormResponse.from(product, null);
    }

    @Transactional(readOnly = true)
    public MyReviewFormResponse getReviewUpdateForm(Long reviewId, Long memberId) {
        validateMember(memberId);
        validateReview(reviewId);
        
        Review review = findReview(reviewId);
        validateReviewAccess(review, memberId);

        Product product = findProductOrNull(review.getProduct().getId());
        return MyReviewFormResponse.from(product, review);
    }

    // Validation methods
    private void validateMember(Long memberId) {
        if (memberId == null) {
            throw new MemberException(ErrorCode.USER_NOT_FOUND);
        }
        if (!memberRepository.existsById(memberId)) {
            throw new MemberException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateOrder(Long orderId) {
        if (orderId == null) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!orderRepository.existsById(orderId)) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
    }

    private void validateReview(Long reviewId) {
        if (reviewId == null) {
            throw new ReviewException(ErrorCode.REVIEW_NOT_FOUND);
        }
        if (!reviewRepository.existsById(reviewId)) {
            throw new ReviewException(ErrorCode.REVIEW_NOT_FOUND);
        }
    }

    private void validateProduct(Long productId) {
        if (productId == null) {
            throw new ReviewException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!productRepository.existsById(productId)) {
            throw new ReviewException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ReviewException(ErrorCode.ORDER_NOT_FOUND));
    }

    private Review findReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private Product findProduct(Long productId) {
        return productRepository.findByIdIncludingDeleted(productId)
                .orElseThrow(() -> new ReviewException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Product findProductOrNull(Long productId) {
        return productRepository.findByIdIncludingDeleted(productId).orElse(null);
    }

    private void validateOrderAccess(Order order, Long memberId) {
        if (!order.getMember().getId().equals(memberId)) {
            throw new ReviewException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateOrderStatus(Order order) {
        if (order.getOrderStatus() != OrderStatus.SUCCESS) {
            throw new ReviewException(ErrorCode.ORDER_NOT_COMPLETED);
        }
    }

    private void validateReviewNotExists(Long orderId) {
        if (reviewRepository.existsByOrderId(orderId)) {
            throw new ReviewException(ErrorCode.ALREADY_REVIEWED);
        }
    }

    private void validateReviewAccess(Review review, Long memberId) {
        if (!review.isWrittenBy(memberId)) {
            throw new ReviewException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateProductAccess(Long productId, Long sellerId) {
        if (productRepository.findByIdAndMemberIdIncludingDeleted(productId, sellerId).isEmpty()) {
            throw new ReviewException(ErrorCode.ACCESS_DENIED);
        }
    }

    private Long extractProductId(Order order) {
        return order.getOrderItems().stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new ReviewException(ErrorCode.ORDER_EMPTY));
    }
}
