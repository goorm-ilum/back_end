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
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.OrderException;
import com.talktrip.talktrip.global.exception.ReviewException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReviewService reviewService;

    private Member member;
    private Product product;
    private Order order;
    private OrderItem orderItem;
    private Review review;
    private ReviewRequest reviewRequest;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .Id(1L)
                .accountEmail("test@test.com")
                .name("테스트유저")
                .nickname("테스트유저")
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();

        product = Product.builder()
                .id(1L)
                .productName("제주도 여행")
                .description("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .build();

        orderItem = OrderItem.builder()
                .id(1L)
                .productId(1L)
                .build();

        order = Order.builder()
                .id(1L)
                .member(member)
                .orderStatus(OrderStatus.SUCCESS)
                .orderItems(List.of(orderItem))
                .build();

        review = Review.builder()
                .id(1L)
                .member(member)
                .product(product)
                .order(order)
                .comment("좋은 여행이었습니다")
                .reviewStar(4.5)
                .build();

        reviewRequest = new ReviewRequest("좋은 여행이었습니다", 4.5);
    }

    @Test
    @DisplayName("리뷰를 성공적으로 생성한다")
    void createReview_Success() {
        // given
        Long orderId = 1L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByOrderId(orderId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        // when
        reviewService.createReview(orderId, memberId, reviewRequest);

        // then
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 리뷰 생성 시 예외가 발생한다")
    void createReview_MemberNotFound() {
        // given
        Long orderId = 1L;
        Long nonExistentMemberId = 999L;

        when(memberRepository.existsById(nonExistentMemberId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(orderId, nonExistentMemberId, reviewRequest))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("null 사용자 ID로 리뷰 생성 시 예외가 발생한다")
    void createReview_NullMemberId() {
        // given
        Long orderId = 1L;
        Long memberId = null;

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(orderId, memberId, reviewRequest))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("null 리뷰 ID로 리뷰 수정 시 예외가 발생한다")
    void updateReview_NullReviewId() {
        Long memberId = 1L;
        when(memberRepository.existsById(memberId)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.updateReview(null, memberId, reviewRequest))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("null 리뷰 ID로 리뷰 삭제 시 예외가 발생한다")
    void deleteReview_NullReviewId() {
        Long memberId = 1L;
        when(memberRepository.existsById(memberId)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.deleteReview(null, memberId))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("null 주문 ID로 리뷰 생성 폼 조회 시 예외가 발생한다")
    void getReviewCreateForm_NullOrderId() {
        Long memberId = 1L;
        when(memberRepository.existsById(memberId)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.getReviewCreateForm(null, memberId))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("null 리뷰 ID로 리뷰 수정 폼 조회 시 예외가 발생한다")
    void getReviewUpdateForm_NullReviewId() {
        Long memberId = 1L;
        when(memberRepository.existsById(memberId)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.getReviewUpdateForm(null, memberId))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 주문으로 리뷰 생성 시 예외가 발생한다")
    void createReview_OrderNotFound() {
        // given
        Long nonExistentOrderId = 999L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(orderRepository.existsById(nonExistentOrderId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(nonExistentOrderId, memberId, reviewRequest))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("null 주문 ID로 리뷰 생성 시 예외가 발생한다")
    void createReview_NullOrderId() {
        // given
        Long orderId = null;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(orderId, memberId, reviewRequest))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 리뷰 생성 시 예외가 발생한다")
    void createReview_ProductNotFound() {
        // given
        Long orderId = 1L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productRepository.existsById(1L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(orderId, memberId, reviewRequest))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 주문으로 리뷰 생성 시 예외가 발생한다")
    void createReview_AccessDenied() {
        // given
        Long orderId = 1L;
        Long differentMemberId = 2L;

        Member differentMember = Member.builder()
                .Id(2L)
                .accountEmail("different@test.com")
                .name("다른유저")
                .nickname("다른유저")
                .build();

        when(memberRepository.existsById(differentMemberId)).thenReturn(true);
        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(memberRepository.findById(differentMemberId)).thenReturn(Optional.of(differentMember));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(orderId, differentMemberId, reviewRequest))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("완료되지 않은 주문으로 리뷰 생성 시 예외가 발생한다")
    void createReview_OrderNotCompleted() {
        // given
        Long orderId = 1L;
        Long memberId = 1L;
        Order pendingOrder = Order.builder()
                .id(1L)
                .member(member)
                .orderStatus(OrderStatus.PENDING)
                .orderItems(List.of(orderItem))
                .build();

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(orderId, memberId, reviewRequest))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_COMPLETED);
    }

    @Test
    @DisplayName("이미 리뷰가 존재하는 주문으로 리뷰 생성 시 예외가 발생한다")
    void createReview_AlreadyReviewed() {
        // given
        Long orderId = 1L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(reviewRepository.existsByOrderId(orderId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(orderId, memberId, reviewRequest))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_REVIEWED);
    }

    @Test
    @DisplayName("리뷰를 성공적으로 수정한다")
    void updateReview_Success() {
        // given
        Long reviewId = 1L;
        Long memberId = 1L;
        ReviewRequest updateRequest = new ReviewRequest("수정된 리뷰입니다", 5.0);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(reviewRepository.existsById(reviewId)).thenReturn(true);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // when
        reviewService.updateReview(reviewId, memberId, updateRequest);

        // then
        verify(reviewRepository).findById(reviewId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 리뷰 수정 시 예외가 발생한다")
    void updateReview_MemberNotFound() {
        // given
        Long reviewId = 1L;
        Long nonExistentMemberId = 999L;

        when(memberRepository.existsById(nonExistentMemberId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(reviewId, nonExistentMemberId, reviewRequest))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 수정 시 예외가 발생한다")
    void updateReview_ReviewNotFound() {
        // given
        Long nonExistentReviewId = 999L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(reviewRepository.existsById(nonExistentReviewId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(nonExistentReviewId, memberId, reviewRequest))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 리뷰 수정 시 예외가 발생한다")
    void updateReview_AccessDenied() {
        // given
        Long reviewId = 1L;
        Long differentMemberId = 2L;

        when(memberRepository.existsById(differentMemberId)).thenReturn(true);
        when(reviewRepository.existsById(reviewId)).thenReturn(true);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(reviewId, differentMemberId, reviewRequest))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("리뷰를 성공적으로 삭제한다")
    void deleteReview_Success() {
        // given
        Long reviewId = 1L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(reviewRepository.existsById(reviewId)).thenReturn(true);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // when
        reviewService.deleteReview(reviewId, memberId);

        // then
        verify(reviewRepository).delete(any(Review.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 리뷰 삭제 시 예외가 발생한다")
    void deleteReview_MemberNotFound() {
        // given
        Long reviewId = 1L;
        Long nonExistentMemberId = 999L;

        when(memberRepository.existsById(nonExistentMemberId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, nonExistentMemberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 삭제 시 예외가 발생한다")
    void deleteReview_ReviewNotFound() {
        // given
        Long nonExistentReviewId = 999L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(reviewRepository.existsById(nonExistentReviewId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(nonExistentReviewId, memberId))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("내 리뷰 목록을 성공적으로 조회한다")
    void getMyReviews_Success() {
        // given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = new PageImpl<>(List.of(review), pageable, 1);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(reviewRepository.findByMemberIdWithProduct(memberId, pageable)).thenReturn(reviewPage);

        // when
        Page<ReviewResponse> result = reviewService.getMyReviews(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).reviewId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).comment()).isEqualTo("좋은 여행이었습니다");
        assertThat(result.getContent().get(0).reviewStar()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 리뷰 목록 조회 시 예외가 발생한다")
    void getMyReviews_UserNotFound() {
        // given
        Long nonExistentMemberId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(nonExistentMemberId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.getMyReviews(nonExistentMemberId, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("판매자 상품의 리뷰 목록을 성공적으로 조회한다")
    void getReviewsForAdminProduct_Success() {
        // given
        Long sellerId = 1L;
        Long productId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = new PageImpl<>(List.of(review), pageable, 1);

        when(memberRepository.existsById(sellerId)).thenReturn(true);
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productRepository.findByIdAndMemberIdIncludingDeleted(productId, sellerId)).thenReturn(Optional.of(product));
        when(reviewRepository.findByProductIdWithPaging(productId, pageable)).thenReturn(reviewPage);
        when(productRepository.findByIdIncludingDeleted(productId)).thenReturn(Optional.of(product));

        // when
        Page<ReviewResponse> result = reviewService.getReviewsForAdminProduct(sellerId, productId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).reviewId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 판매자로 리뷰 목록 조회 시 예외가 발생한다")
    void getReviewsForAdminProduct_SellerNotFound() {
        // given
        Long nonExistentSellerId = 999L;
        Long productId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(nonExistentSellerId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.getReviewsForAdminProduct(nonExistentSellerId, productId, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 리뷰 목록 조회 시 예외가 발생한다")
    void getReviewsForAdminProduct_ProductNotFound() {
        // given
        Long sellerId = 1L;
        Long nonExistentProductId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(sellerId)).thenReturn(true);
        when(productRepository.existsById(nonExistentProductId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.getReviewsForAdminProduct(sellerId, nonExistentProductId, pageable))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("판매자 리뷰 목록 조회 - null 상품 ID 시 PRODUCT_NOT_FOUND")
    void getReviewsForAdminProduct_NullProductId() {
        // given
        Long sellerId = 1L;
        Long productId = null;
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(sellerId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.getReviewsForAdminProduct(sellerId, productId, pageable))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("접근 권한이 없는 상품의 리뷰 목록 조회 시 예외가 발생한다")
    void getReviewsForAdminProduct_AccessDenied() {
        // given
        Long sellerId = 1L;
        Long productId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(sellerId)).thenReturn(true);
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productRepository.findByIdAndMemberIdIncludingDeleted(productId, sellerId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.getReviewsForAdminProduct(sellerId, productId, pageable))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("리뷰 생성 폼을 성공적으로 조회한다")
    void getReviewCreateForm_Success() {
        // given
        Long orderId = 1L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByOrderId(orderId)).thenReturn(false);

        // when
        MyReviewFormResponse result = reviewService.getReviewCreateForm(orderId, memberId);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 리뷰 생성 폼 조회 시 예외가 발생한다")
    void getReviewCreateForm_MemberNotFound() {
        // given
        Long orderId = 1L;
        Long nonExistentMemberId = 999L;

        when(memberRepository.existsById(nonExistentMemberId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.getReviewCreateForm(orderId, nonExistentMemberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 주문으로 리뷰 생성 폼 조회 시 예외가 발생한다")
    void getReviewCreateForm_OrderNotFound() {
        // given
        Long nonExistentOrderId = 999L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(orderRepository.existsById(nonExistentOrderId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.getReviewCreateForm(nonExistentOrderId, memberId))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("리뷰 수정 폼을 성공적으로 조회한다")
    void getReviewUpdateForm_Success() {
        // given
        Long reviewId = 1L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(reviewRepository.existsById(reviewId)).thenReturn(true);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(productRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(product));

        // when
        MyReviewFormResponse result = reviewService.getReviewUpdateForm(reviewId, memberId);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 리뷰 수정 폼 조회 시 예외가 발생한다")
    void getReviewUpdateForm_MemberNotFound() {
        // given
        Long reviewId = 1L;
        Long nonExistentMemberId = 999L;

        when(memberRepository.existsById(nonExistentMemberId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.getReviewUpdateForm(reviewId, nonExistentMemberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 리뷰로 수정 폼 조회 시 예외가 발생한다")
    void getReviewUpdateForm_ReviewNotFound() {
        // given
        Long nonExistentReviewId = 999L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(reviewRepository.existsById(nonExistentReviewId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.getReviewUpdateForm(nonExistentReviewId, memberId))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    // ===== 추가: validate 통과 후 find 단계에서의 NOT_FOUND 예외들 =====

    @Test
    @DisplayName("createReview: member exists지만 findById 없음 → ReviewException(USER_NOT_FOUND)")
    void createReview_FindMemberNotFound() {
        Long orderId = 1L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(orderId, memberId, reviewRequest))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("createReview: order exists지만 findById 없음 → ReviewException(ORDER_NOT_FOUND)")
    void createReview_FindOrderNotFound() {
        Long orderId = 1L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(orderId, memberId, reviewRequest))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("createReview: product exists지만 findByIdIncludingDeleted 없음 → ReviewException(PRODUCT_NOT_FOUND)")
    void createReview_FindProductNotFound() {
        Long orderId = 1L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(orderId, memberId, reviewRequest))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("updateReview: review exists지만 findById 없음 → ReviewException(REVIEW_NOT_FOUND)")
    void updateReview_FindReviewNotFound() {
        Long reviewId = 1L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(reviewRepository.existsById(reviewId)).thenReturn(true);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(reviewId, memberId, reviewRequest))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteReview: review exists지만 findById 없음 → ReviewException(REVIEW_NOT_FOUND)")
    void deleteReview_FindReviewNotFound() {
        Long reviewId = 1L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(reviewRepository.existsById(reviewId)).thenReturn(true);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, memberId))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("getReviewCreateForm: order exists지만 findById 없음 → ReviewException(ORDER_NOT_FOUND)")
    void getReviewCreateForm_FindOrderNotFound() {
        Long orderId = 1L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewCreateForm(orderId, memberId))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("리뷰 생성 - 주문에 상품이 없어 ORDER_EMPTY")
    void createReview_OrderEmpty_ThrowsException() {
        // given
        Long orderId = 1L;
        Long memberId = 1L;

        Order orderWithoutProduct = Order.builder()
                .id(orderId)
                .member(member)
                .orderStatus(OrderStatus.SUCCESS)
                .orderItems(java.util.List.of(
                        OrderItem.builder().id(10L).productId(null).build()
                ))
                .build();

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(memberRepository.findById(memberId)).thenReturn(java.util.Optional.of(member));
        when(orderRepository.findById(orderId)).thenReturn(java.util.Optional.of(orderWithoutProduct));
        when(reviewRepository.existsByOrderId(orderId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(orderId, memberId, reviewRequest))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_EMPTY);
    }

    @Test
    @DisplayName("getReviewUpdateForm: review exists지만 findById 없음 → ReviewException(REVIEW_NOT_FOUND)")
    void getReviewUpdateForm_FindReviewNotFound() {
        Long reviewId = 1L;
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(reviewRepository.existsById(reviewId)).thenReturn(true);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewUpdateForm(reviewId, memberId))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }
}
