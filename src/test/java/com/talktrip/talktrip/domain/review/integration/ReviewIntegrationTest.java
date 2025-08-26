package com.talktrip.talktrip.domain.review.integration;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.Gender;
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
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.domain.review.service.ReviewService;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.exception.*;
import com.talktrip.talktrip.global.repository.CountryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
class ReviewIntegrationTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Member member1;
    private Member member2;
    private Member member3;
    private Country country;
    private Product product1;
    private Product product2;
    private Order order1;
    private Order order2;
    private Order order3;
    private Review review1;
    private Review review2;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 생성
        country = Country.builder()
                .id(1L)
                .name("대한민국")
                .continent("아시아")
                .build();
        countryRepository.save(country);

        member1 = Member.builder()
                .accountEmail("test1@test.com")
                .name("테스트유저1")
                .nickname("테스터1")
                .gender(Gender.M)
                .birthday(LocalDate.of(1990, 1, 1))
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(member1);

        member2 = Member.builder()
                .accountEmail("test2@test.com")
                .name("테스트유저2")
                .nickname("테스터2")
                .gender(Gender.F)
                .birthday(LocalDate.of(1995, 5, 5))
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(member2);

        member3 = Member.builder()
                .accountEmail("test3@test.com")
                .name("테스트유저3")
                .nickname("테스터3")
                .gender(Gender.F)
                .birthday(LocalDate.of(1997, 5, 5))
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(member3);

        product1 = Product.builder()
                .productName("제주도 여행")
                .description("아름다운 제주도 여행 상품입니다")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .member(member1)
                .country(country)
                .build();
        productRepository.save(product1);

        product2 = Product.builder()
                .productName("서울 도시 여행")
                .description("서울의 다양한 관광지를 둘러보는 상품")
                .thumbnailImageUrl("https://example.com/seoul.jpg")
                .member(member1)
                .country(country)
                .build();
        productRepository.save(product2);

        // 주문 생성
        order1 = Order.builder()
                .member(member2)
                .orderDate(LocalDate.now().minusDays(7))
                .totalPrice(100000)
                .orderStatus(OrderStatus.SUCCESS)
                .orderCode("ORDER-001")
                .build();
        orderRepository.save(order1);

        OrderItem orderItem1 = OrderItem.builder()
                .order(order1)
                .productId(product1.getId())
                .quantity(1)
                .price(100000)
                .build();
        order1.addOrderItem(orderItem1);

        order2 = Order.builder()
                .member(member1)
                .orderDate(LocalDate.now().minusDays(5))
                .totalPrice(150000)
                .orderStatus(OrderStatus.SUCCESS)
                .orderCode("ORDER-002")
                .build();
        orderRepository.save(order2);

        OrderItem orderItem2 = OrderItem.builder()
                .order(order2)
                .productId(product1.getId())
                .quantity(1)
                .price(150000)
                .build();
        order2.addOrderItem(orderItem2);

        order3 = Order.builder()
                .member(member3)
                .orderDate(LocalDate.now().minusDays(5))
                .totalPrice(150000)
                .orderStatus(OrderStatus.SUCCESS)
                .orderCode("ORDER-003")
                .build();
        orderRepository.save(order3);

        OrderItem orderItem3 = OrderItem.builder()
                .order(order3)
                .productId(product2.getId())
                .quantity(1)
                .price(150000)
                .build();
        order3.addOrderItem(orderItem3);

        review1 = Review.builder()
                .product(product1)
                .member(member2)
                .order(order1)
                .reviewStar(4.5)
                .comment("정말 좋은 여행이었습니다!")
                .build();
        reviewRepository.save(review1);

        review2 = Review.builder()
                .product(product1)
                .member(member1)
                .order(order2)
                .reviewStar(5.0)
                .comment("완벽한 여행이었어요!")
                .build();
        reviewRepository.save(review2);
    }

    @Test
    @DisplayName("리뷰를 성공적으로 생성한다")
    void createReview_Success() {
        // given
        Long orderId = order3.getId();
        Long memberId = member3.getId();
        ReviewRequest request = new ReviewRequest("서울 여행도 좋았습니다!", 4.0);

        // when
        reviewService.createReview(orderId, memberId, request);

        // then
        // 실제 DB에서 확인
        List<Review> reviews = reviewRepository.findAll();
        assertThat(reviews).hasSize(3);
        assertThat(reviews).extracting("comment").contains("서울 여행도 좋았습니다!");
    }

    @Test
    @DisplayName("리뷰 생성 시 존재하지 않는 주문이면 예외가 발생한다")
    void createReview_NonExistentOrder_ThrowsException() {
        // given
        Long nonExistentOrderId = 999L;
        Long memberId = member2.getId();
        ReviewRequest request = new ReviewRequest("테스트 리뷰", 4.0);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(nonExistentOrderId, memberId, request))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("리뷰 생성 시 존재하지 않는 회원이면 예외가 발생한다")
    void createReview_NonExistentMember_ThrowsException() {
        // given
        Long orderId = order1.getId();
        Long nonExistentMemberId = 999L;
        ReviewRequest request = new ReviewRequest("테스트 리뷰", 4.0);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(orderId, nonExistentMemberId, request))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("리뷰를 성공적으로 수정한다")
    void updateReview_Success() {
        // given
        Long reviewId = review1.getId();
        Long memberId = member2.getId();
        ReviewRequest request = new ReviewRequest("수정된 리뷰입니다!", 5.0);

        // when
        reviewService.updateReview(reviewId, memberId, request);

        // then
        // 실제 DB에서 확인
        Review updatedReview = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(updatedReview.getReviewStar()).isEqualTo(5.0);
        assertThat(updatedReview.getComment()).isEqualTo("수정된 리뷰입니다!");
    }

    @Test
    @DisplayName("리뷰 수정 시 존재하지 않는 리뷰이면 예외가 발생한다")
    void updateReview_NonExistentReview_ThrowsException() {
        // given
        Long nonExistentReviewId = 999L;
        Long memberId = member2.getId();
        ReviewRequest request = new ReviewRequest("수정된 리뷰입니다!", 5.0);

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(nonExistentReviewId, memberId, request))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("리뷰 수정 시 다른 회원의 리뷰이면 예외가 발생한다")
    void updateReview_OtherMemberReview_ThrowsException() {
        // given
        Long reviewId = review1.getId();
        Long otherMemberId = member1.getId(); // review1은 member2의 리뷰
        ReviewRequest request = new ReviewRequest("수정된 리뷰입니다!", 5.0);

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(reviewId, otherMemberId, request))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("리뷰를 성공적으로 삭제한다")
    void deleteReview_Success() {
        // given
        Long reviewId = review1.getId();
        Long memberId = member2.getId();

        // when
        reviewService.deleteReview(reviewId, memberId);

        // then
        // 실제 DB에서 확인
        List<Review> reviews = reviewRepository.findAll();
        assertThat(reviews).hasSize(1);
        assertThat(reviews).extracting("id").doesNotContain(reviewId);
    }

    @Test
    @DisplayName("리뷰 삭제 시 존재하지 않는 리뷰이면 예외가 발생한다")
    void deleteReview_NonExistentReview_ThrowsException() {
        // given
        Long nonExistentReviewId = 999L;
        Long memberId = member2.getId();

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(nonExistentReviewId, memberId))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("리뷰 삭제 시 다른 회원의 리뷰이면 예외가 발생한다")
    void deleteReview_OtherMemberReview_ThrowsException() {
        // given
        Long reviewId = review1.getId();
        Long otherMemberId = member1.getId(); // review1은 member2의 리뷰

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, otherMemberId))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("내 리뷰 목록을 성공적으로 조회한다")
    void getMyReviews_Success() {
        // given
        Long memberId = member2.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ReviewResponse> result = reviewService.getMyReviews(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        ReviewResponse review = result.getContent().get(0);
        assertThat(review.reviewId()).isEqualTo(review1.getId());
        assertThat(review.reviewStar()).isEqualTo(4.5);
        assertThat(review.comment()).isEqualTo("정말 좋은 여행이었습니다!");
        assertThat(review.nickName()).isEqualTo("테스터2");
    }

    @Test
    @DisplayName("내 리뷰 목록 조회 시 존재하지 않는 회원이면 예외가 발생한다")
    void getMyReviews_NonExistentMember_ThrowsException() {
        // given
        Long nonExistentMemberId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> reviewService.getMyReviews(nonExistentMemberId, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("내 리뷰 목록 조회 시 페이징이 올바르게 적용된다")
    void getMyReviews_WithPaging() {
        // given
        Long memberId = member1.getId(); // member1은 1개의 리뷰를 가짐
        Pageable pageable = PageRequest.of(0, 1); // 페이지 크기 1

        // when
        Page<ReviewResponse> result = reviewService.getMyReviews(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("상품별 평균 별점을 일괄 조회한다")
    void fetchAvgStarsByProductIds_Success() {
        // given
        List<Long> productIds = List.of(product1.getId(), product2.getId());

        // when
        Map<Long, Double> result = reviewRepository.fetchAvgStarsByProductIds(productIds);

        // then
        assertThat(result).hasSize(1); // product2는 리뷰가 없으므로 제외
        assertThat(result.get(product1.getId())).isEqualTo(4.8); // (4.5 + 5.0) / 2
    }

    @Test
    @DisplayName("상품별 평균 별점 조회 시 리뷰가 없는 상품은 제외된다")
    void fetchAvgStarsByProductIds_ExcludesProductsWithoutReviews() {
        // given
        List<Long> productIds = List.of(product2.getId()); // product2는 리뷰가 없음

        // when
        Map<Long, Double> result = reviewRepository.fetchAvgStarsByProductIds(productIds);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상품별 평균 별점 조회 시 빈 리스트를 전달하면 빈 결과를 반환한다")
    void fetchAvgStarsByProductIds_EmptyList() {
        // given
        List<Long> productIds = List.of();

        // when
        Map<Long, Double> result = reviewRepository.fetchAvgStarsByProductIds(productIds);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상품별 평균 별점 조회 시 null 리스트를 전달하면 빈 결과를 반환한다")
    void fetchAvgStarsByProductIds_NullList() {
        // given
        List<Long> productIds = null;

        // when
        Map<Long, Double> result = reviewRepository.fetchAvgStarsByProductIds(productIds);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상품 ID로 리뷰를 페이징하여 조회한다")
    void findByProductId_WithPaging() {
        // given
        Long productId = product1.getId();
        Pageable pageable = PageRequest.of(0, 1); // 페이지 크기 1

        // when
        Page<Review> result = reviewRepository.findByProductId(productId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isFalse();
    }

    @Test
    @DisplayName("상품 ID로 리뷰 조회 시 리뷰가 없으면 빈 페이지를 반환한다")
    void findByProductId_NoReviews() {
        // given
        Long productId = product2.getId(); // product2는 리뷰가 없음
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Review> result = reviewRepository.findByProductId(productId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }
}
