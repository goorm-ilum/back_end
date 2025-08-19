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
import com.talktrip.talktrip.domain.review.dto.event.ReviewEventDTO;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ReviewException;
import com.talktrip.talktrip.global.kafka.ReviewProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final ReviewProducer reviewProducer;

    private final ObjectMapper objectMapper;

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

        // Kafka 메시지 발송
        try {
            ReviewEventDTO eventDTO = new ReviewEventDTO(
                    review.getId(),
                    product.getId(),
                    review.getComment()
            );
            String reviewJson = objectMapper.writeValueAsString(eventDTO);
            reviewProducer.sendReview(reviewJson);
        } catch (JsonProcessingException e) {
            log.error("리뷰 직렬화 실패", e);
        }
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
    public Page<ReviewResponse> getReviewsForAdminProduct(
            Long sellerId,
            Long productId,
            Pageable pageable
    ) {
        productRepository.findByIdAndMemberIdIncludingDeleted(productId, sellerId)
                .orElseThrow(() -> new ReviewException(ErrorCode.ACCESS_DENIED));

        List<Review> allReviews = reviewRepository.findByProductIdIncludingDeleted(productId);


        List<Review> sorted = allReviews.stream()
                .sorted(getComparator(pageable.getSort()))
                .toList();

        int offset = (int) pageable.getOffset();
        int toIndex = Math.min(offset + pageable.getPageSize(), sorted.size());
        List<Review> paged = (offset > sorted.size()) ? List.of() : sorted.subList(offset, toIndex);

        Product product = productRepository.findByIdIncludingDeleted(productId).orElse(null);

        return new PageImpl<>(
                paged.stream().map(r -> ReviewResponse.from(r, product)).toList(),
                pageable,
                sorted.size()
        );
    }

    private Comparator<Review> getComparator(Sort sort) {
        Comparator<Review> comparator = Comparator.comparing(Review::getUpdatedAt); // 기본값

        for (Sort.Order order : sort) {
            switch (order.getProperty()) {
                case "updatedAt" -> comparator = Comparator.comparing(Review::getUpdatedAt);
                case "reviewStar" -> comparator = Comparator.comparing(Review::getReviewStar);
                default -> throw new IllegalArgumentException("Invalid sort property: " + order.getProperty());
            }

            if (order.getDirection().isDescending()) {
                comparator = comparator.reversed();
            }
        }
        return comparator;
    }
}
