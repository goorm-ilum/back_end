package com.talktrip.talktrip.domain.review.service;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @InjectMocks ReviewService reviewService;
    @Mock ReviewRepository reviewRepository;
    @Mock ProductRepository productRepository;
    @Mock MemberRepository memberRepository;
    @Mock OrderRepository orderRepository;

    private Member user() {
        return Member.builder()
                .Id(USER_ID).accountEmail(USER_EMAIL)
                .memberRole(MemberRole.U).memberState(MemberState.A)
                .build();
    }

    private Member otherUser() {
        return Member.builder()
                .Id(USER_ID2).accountEmail(USER2_EMAIL)
                .memberRole(MemberRole.U).memberState(MemberState.A)
                .build();
    }

    private Product product() {
        return Product.builder()
                .id(PRODUCT_ID)
                .productName(PRODUCT_NAME_1)
                .description(DESC)
                .deleted(false)
                .build();
    }

    private Order order(Member u, OrderStatus status, Long productId) {
        Order o = Order.builder()
                .id(ORDER_ID)
                .member(u)
                .orderStatus(status)
                .build();
        o.getOrderItems().add(OrderItem.builder().order(o).productId(productId).build());
        return o;
    }
    private Order order(OrderStatus status, Long productId) { return order(user(), status, productId); }

    @Nested @DisplayName("createReview(orderId, memberId, request)")
    class Create {

        @Test @DisplayName("정상: SUCCESS & 기존 리뷰 없음 -> 저장")
        void ok() {
            Order o = order(OrderStatus.SUCCESS, PRODUCT_ID);

            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(o));
            given(reviewRepository.existsByOrderId(ORDER_ID)).willReturn(false);
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.of(product()));

            reviewService.createReview(ORDER_ID, USER_ID, new ReviewRequest(COMMENT_TEST, STAR_4_0));

            then(reviewRepository).should().save(any(Review.class));
        }

        @Test @DisplayName("USER_NOT_FOUND")
        void userMissing() {
            given(memberRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(ORDER_ID, USER_ID, new ReviewRequest(COMMENT_TEST, STAR_4_0)))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test @DisplayName("ORDER_NOT_FOUND")
        void orderMissing() {
            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(ORDER_ID, USER_ID, new ReviewRequest(COMMENT_TEST, STAR_4_0)))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test @DisplayName("ACCESS_DENIED: 주문 소유자 아님")
        void accessDenied() {
            Order o = order(otherUser(), OrderStatus.SUCCESS, PRODUCT_ID);

            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(o));

            assertThatThrownBy(() -> reviewService.createReview(ORDER_ID, USER_ID, new ReviewRequest(COMMENT_TEST, STAR_4_0)))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test @DisplayName("ORDER_NOT_COMPLETED: 주문 상태 성공 아님")
        void notCompleted() {
            Order o = order(OrderStatus.PENDING, PRODUCT_ID);

            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(o));

            assertThatThrownBy(() -> reviewService.createReview(ORDER_ID, USER_ID, new ReviewRequest(COMMENT_TEST, STAR_4_0)))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ORDER_NOT_COMPLETED);
        }

        @Test @DisplayName("ALREADY_REVIEWED: 이미 리뷰 있음")
        void alreadyReviewed() {
            Order o = order(OrderStatus.SUCCESS, PRODUCT_ID);

            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(o));
            given(reviewRepository.existsByOrderId(ORDER_ID)).willReturn(true);

            assertThatThrownBy(() -> reviewService.createReview(ORDER_ID, USER_ID, new ReviewRequest(COMMENT_TEST, STAR_4_0)))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_REVIEWED);
        }

        @Test @DisplayName("ORDER_EMPTY: 주문에 상품 없음")
        void orderEmpty() {
            Order o = order(OrderStatus.SUCCESS, null);

            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(o));
            given(reviewRepository.existsByOrderId(ORDER_ID)).willReturn(false);

            assertThatThrownBy(() -> reviewService.createReview(ORDER_ID, USER_ID, new ReviewRequest(COMMENT_TEST, STAR_4_0)))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ORDER_EMPTY);
        }

        @Test @DisplayName("PRODUCT_NOT_FOUND: 제품 없음")
        void productMissing() {
            Order o = order(OrderStatus.SUCCESS, PRODUCT_ID);

            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(o));
            given(reviewRepository.existsByOrderId(ORDER_ID)).willReturn(false);
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(ORDER_ID, USER_ID, new ReviewRequest(COMMENT_TEST, STAR_4_0)))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    @Nested @DisplayName("updateReview(reviewId, memberId, request)")
    class Update {

        @Test @DisplayName("정상: 본인 리뷰 수정")
        void ok() {
            Product p = product();
            Review r = Review.builder()
                    .id(REVIEW_ID)
                    .member(user())
                    .product(p)
                    .order(order(OrderStatus.SUCCESS, PRODUCT_ID))
                    .comment("old review")
                    .reviewStar(STAR_2_0)
                    .build();

            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(r));

            reviewService.updateReview(REVIEW_ID, USER_ID, new ReviewRequest(COMMENT_TEST, STAR_4_5));

            assertThat(r.getComment()).isEqualTo(COMMENT_TEST);
            assertThat(r.getReviewStar()).isEqualTo(STAR_4_5);
        }

        @Test @DisplayName("REVIEW_NOT_FOUND")
        void reviewMissing() {
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.updateReview(REVIEW_ID, USER_ID, new ReviewRequest(COMMENT_TEST, STAR_4_5)))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
        }

        @Test @DisplayName("ACCESS_DENIED: 본인 아님")
        void forbidden() {
            Product p = product();
            Review r = Review.builder()
                    .id(REVIEW_ID)
                    .member(otherUser())
                    .product(p)
                    .order(order(OrderStatus.SUCCESS, PRODUCT_ID))
                    .comment("old")
                    .reviewStar(STAR_2_0)
                    .build();

            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(r));

            assertThatThrownBy(() -> reviewService.updateReview(REVIEW_ID, USER_ID, new ReviewRequest(COMMENT_TEST, STAR_4_5)))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }

    @Nested @DisplayName("deleteReview(reviewId, memberId)")
    class Delete {

        @Test @DisplayName("정상: 본인 리뷰 삭제")
        void ok() {
            Product p = product();
            Review r = Review.builder()
                    .id(REVIEW_ID)
                    .member(user())
                    .product(p)
                    .order(order(OrderStatus.SUCCESS, PRODUCT_ID))
                    .comment(COMMENT_TEST)
                    .reviewStar(STAR_3_0)
                    .build();

            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(r));

            reviewService.deleteReview(REVIEW_ID, USER_ID);

            then(reviewRepository).should().delete(r);
        }

        @Test @DisplayName("REVIEW_NOT_FOUND")
        void missing() {
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.deleteReview(REVIEW_ID, USER_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
        }

        @Test @DisplayName("ACCESS_DENIED: 본인 아님")
        void forbidden() {
            Product p = product();
            Review r = Review.builder()
                    .id(REVIEW_ID)
                    .member(otherUser())
                    .product(p)
                    .order(order(OrderStatus.SUCCESS, PRODUCT_ID))
                    .comment(COMMENT_TEST)
                    .reviewStar(STAR_3_0)
                    .build();

            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(r));

            assertThatThrownBy(() -> reviewService.deleteReview(REVIEW_ID, USER_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }

    @Nested @DisplayName("getMyReviews(memberId, pageable)")
    class MyReviews {

        @Test @DisplayName("USER_NOT_FOUND")
        void userMissing() {
            given(memberRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.getMyReviews(USER_ID, PageRequest.of(PAGE_0, SIZE_9)))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test @DisplayName("빈 페이지 매핑")
        void empty() {
            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            Page<Review> page = new PageImpl<>(List.of(), PageRequest.of(PAGE_0, SIZE_9), 0);
            given(reviewRepository.findByMemberId(eq(USER_ID), any(Pageable.class))).willReturn(page);

            Page<ReviewResponse> res = reviewService.getMyReviews(USER_ID, PageRequest.of(PAGE_0, SIZE_9));

            assertThat(res.getTotalElements()).isZero();
            assertThat(res.getContent()).isEmpty();
        }

        @Test @DisplayName("초과 페이지 → 빈 페이지 반환")
        void overflow_passthrough() {
            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            Pageable overflow = PageRequest.of(PAGE_4, SIZE_9, SORT_BY_UPDATED_DESC);
            Page<Review> page = new PageImpl<>(List.of(), overflow, 2);
            given(reviewRepository.findByMemberId(eq(USER_ID), any(Pageable.class))).willReturn(page);

            Page<ReviewResponse> res = reviewService.getMyReviews(USER_ID, overflow);

            assertThat(res.getContent()).isEmpty();
            assertThat(res.getTotalElements()).isEqualTo(2);
            assertThat(res.getNumber()).isEqualTo(PAGE_4);
            assertThat(res.getSize()).isEqualTo(SIZE_9);
        }

        @Test @DisplayName("데이터 매핑")
        void hasData() {
            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(user()));

            Review r = Review.builder()
                    .id(REVIEW_ID)
                    .member(user())
                    .product(Product.builder().id(PRODUCT_ID).build())
                    .comment(COMMENT_TEST)
                    .reviewStar(STAR_4_0)
                    .build();

            Page<Review> page = new PageImpl<>(List.of(r), PageRequest.of(PAGE_0, SIZE_9), 1);
            given(reviewRepository.findByMemberId(eq(USER_ID), any(Pageable.class))).willReturn(page);
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.of(product()));

            Page<ReviewResponse> res = reviewService.getMyReviews(USER_ID, PageRequest.of(PAGE_0, SIZE_9));
            assertThat(res.getTotalElements()).isEqualTo(1);
            assertThat(res.getContent()).hasSize(1);
        }
    }

    @Nested @DisplayName("getReviewCreateForm(orderId, memberId)")
    class CreateForm {

        @Test @DisplayName("정상 반환 (상품명 노출, 내 리뷰 정보는 null)")
        void ok() {
            Order o = order(OrderStatus.SUCCESS, PRODUCT_ID);

            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(o));
            given(reviewRepository.existsByOrderId(ORDER_ID)).willReturn(false);
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.of(product()));

            MyReviewFormResponse resp = reviewService.getReviewCreateForm(ORDER_ID, USER_ID);

            assertThat(resp.getReviewId()).isNull();
            assertThat(resp.getProductName()).isEqualTo(PRODUCT_NAME_1);
            assertThat(resp.getThumbnailUrl()).isNull();
            assertThat(resp.getMyStar()).isNull();
            assertThat(resp.getMyComment()).isNull();
        }

        @Test @DisplayName("ORDER_NOT_FOUND")
        void orderMissing() {
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.getReviewCreateForm(ORDER_ID, USER_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test @DisplayName("ACCESS_DENIED")
        void accessDenied_whenOrderOwnerIsDifferent() {
            Order other = order(otherUser(), OrderStatus.SUCCESS, PRODUCT_ID);

            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(other));

            assertThatThrownBy(() -> reviewService.getReviewCreateForm(ORDER_ID, USER_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test @DisplayName("ORDER_NOT_COMPLETED")
        void orderNotCompleted_whenOrderStatusIsNotSuccess() {
            Order pending = order(OrderStatus.PENDING, PRODUCT_ID);

            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(pending));

            assertThatThrownBy(() -> reviewService.getReviewCreateForm(ORDER_ID, USER_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ORDER_NOT_COMPLETED);
        }

        @Test @DisplayName("ALREADY_REVIEWED")
        void alreadyReviewed_whenReviewAlreadyExists() {
            Order ok = order(OrderStatus.SUCCESS, PRODUCT_ID);

            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(ok));
            given(reviewRepository.existsByOrderId(ORDER_ID)).willReturn(true);

            assertThatThrownBy(() -> reviewService.getReviewCreateForm(ORDER_ID, USER_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_REVIEWED);
        }
    }

    @Nested @DisplayName("getReviewUpdateForm(reviewId, memberId)")
    class UpdateForm {

        @Test @DisplayName("정상 (내 리뷰 포함)")
        void ok() {
            Review r = Review.builder()
                    .id(REVIEW_ID)
                    .member(user())
                    .product(Product.builder().id(PRODUCT_ID).productName(PRODUCT_NAME_1).build())
                    .comment(COMMENT_TEST)
                    .reviewStar(STAR_4_0)
                    .build();

            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(r));
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.of(product()));

            MyReviewFormResponse resp = reviewService.getReviewUpdateForm(REVIEW_ID, USER_ID);

            assertThat(resp.getReviewId()).isEqualTo(REVIEW_ID);
            assertThat(resp.getProductName()).isEqualTo(PRODUCT_NAME_1);
            assertThat(resp.getMyStar()).isEqualTo(STAR_4_0);
            assertThat(resp.getMyComment()).isEqualTo(COMMENT_TEST);
        }

        @Test @DisplayName("REVIEW_NOT_FOUND")
        void reviewMissing() {
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.getReviewUpdateForm(REVIEW_ID, USER_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
        }

        @Test @DisplayName("ACCESS_DENIED")
        void forbidden() {
            Review r = Review.builder()
                    .id(REVIEW_ID)
                    .member(otherUser())
                    .product(Product.builder().id(PRODUCT_ID).build())
                    .comment("c")
                    .reviewStar(STAR_3_0)
                    .build();

            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(r));

            assertThatThrownBy(() -> reviewService.getReviewUpdateForm(REVIEW_ID, USER_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }

    @Nested @DisplayName("getReviewsForAdminProduct(sellerId, productId, pageable)")
    class AdminProductReviews {

        @Test @DisplayName("ACCESS_DENIED: 내 상품 아님")
        void forbidden() {
            given(productRepository.findByIdAndMemberIdIncludingDeleted(PRODUCT_ID, USER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.getReviewsForAdminProduct(USER_ID, PRODUCT_ID, PageRequest.of(PAGE_0, SIZE_10)))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test @DisplayName("정렬 프로퍼티 검증: 잘못된 프로퍼티 → IllegalArgumentException")
        void invalidSortProperty() {
            given(productRepository.findByIdAndMemberIdIncludingDeleted(PRODUCT_ID, USER_ID))
                    .willReturn(Optional.of(product()));
            given(reviewRepository.findByProductIdIncludingDeleted(PRODUCT_ID)).willReturn(List.of());

            assertThatThrownBy(() -> reviewService.getReviewsForAdminProduct(
                    USER_ID, PRODUCT_ID, PageRequest.of(PAGE_0, SIZE_10, Sort.by("notExistsProperty"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid sort property");
        }

        @Test @DisplayName("정상: reviewStar desc 정렬 + 페이징")
        void ok_sortAndPage() {
            given(productRepository.findByIdAndMemberIdIncludingDeleted(PRODUCT_ID, USER_ID))
                    .willReturn(Optional.of(product()));

            Review r1 = Review.builder().member(user()).id(101L).reviewStar(STAR_3_0).build();
            Review r2 = Review.builder().member(otherUser()).id(102L).reviewStar(STAR_5_0).build();
            given(reviewRepository.findByProductIdIncludingDeleted(PRODUCT_ID))
                    .willReturn(List.of(r1, r2));

            Page<ReviewResponse> page = reviewService.getReviewsForAdminProduct(
                    USER_ID, PRODUCT_ID, PageRequest.of(PAGE_0, SIZE_1, SORT_BY_REVIEW_STAR_DESC));

            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).hasSize(SIZE_1);
        }
    }
}
