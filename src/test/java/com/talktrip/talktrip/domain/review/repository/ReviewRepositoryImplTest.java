package com.talktrip.talktrip.domain.review.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
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
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.global.config.QueryDSLTestConfig;
import com.talktrip.talktrip.global.entity.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDSLTestConfig.class)
@EnableJpaAuditing
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewRepositoryImplTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private JPAQueryFactory queryFactory;

    private Member member;
    private Product product;
    private Order order;
    private OrderItem orderItem;
    private Review review;
    private Country country;

    @BeforeEach
    void setUp() {
        country = Country.builder()
                .id(1L)
                .name("대한민국")
                .build();
        testEntityManager.persist(country);

        member = Member.builder()
                .accountEmail("test@test.com")
                .name("테스트유저")
                .nickname("테스트유저")
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)

                .build();
        memberRepository.save(member);

        product = Product.builder()
                .member(member)
                .productName("제주도 여행")
                .description("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .country(country)
                .build();
        productRepository.save(product);

        orderItem = OrderItem.builder()
                .productId(product.getId())
                .build();

        order = Order.builder()
                .member(member)
                .orderStatus(OrderStatus.SUCCESS)
                .orderItems(List.of(orderItem))
                .orderCode("order-code")
                .build();
        orderRepository.save(order);

        review = Review.builder()
                .member(member)
                .product(product)
                .order(order)
                .comment("좋은 여행이었습니다")
                .reviewStar(4.5)
                .build();
        reviewRepository.save(review);
    }

    @Test
    @DisplayName("상품 ID로 리뷰를 페이징과 함께 조회한다")
    void findByProductIdWithPaging_Success() {
        // given
        Long productId = product.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Review> result = reviewRepository.findByProductIdWithPaging(productId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        Review foundReview = result.getContent().get(0);
        assertThat(foundReview.getProduct().getId()).isEqualTo(productId);
        assertThat(foundReview.getComment()).isEqualTo("좋은 여행이었습니다");
        assertThat(foundReview.getReviewStar()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 리뷰 조회 시 빈 페이지를 반환한다")
    void findByProductIdWithPaging_NoResult() {
        // given
        Long nonExistentProductId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Review> result = reviewRepository.findByProductIdWithPaging(nonExistentProductId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("사용자 ID로 리뷰를 상품 정보와 함께 조회한다")
    void findByMemberIdWithProduct_Success() {
        // given
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Review> result = reviewRepository.findByMemberIdWithProduct(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        Review foundReview = result.getContent().get(0);
        assertThat(foundReview.getMember().getId()).isEqualTo(memberId);
        assertThat(foundReview.getProduct()).isNotNull();
        assertThat(foundReview.getProduct().getProductName()).isEqualTo("제주도 여행");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 리뷰 조회 시 빈 페이지를 반환한다")
    void findByMemberIdWithProduct_NoResult() {
        // given
        Long nonExistentMemberId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Review> result = reviewRepository.findByMemberIdWithProduct(nonExistentMemberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("여러 리뷰가 있을 때 페이징이 올바르게 작동한다")
    void findByProductIdWithPaging_Pagination() {
        // given
        // 추가 리뷰 생성 - 각각 다른 Order 사용
        OrderItem orderItem2 = OrderItem.builder()
                .productId(product.getId())
                .build();

        Order order2 = Order.builder()
                .member(member)
                .orderStatus(OrderStatus.SUCCESS)
                .orderItems(List.of(orderItem2))
                .orderCode("order-code-2")
                .build();
        orderRepository.save(order2);

        Review review2 = Review.builder()
                .member(member)
                .product(product)
                .order(order2)
                .comment("두 번째 리뷰입니다")
                .reviewStar(4.0)
                .build();
        reviewRepository.save(review2);

        OrderItem orderItem3 = OrderItem.builder()
                .productId(product.getId())
                .build();

        Order order3 = Order.builder()
                .member(member)
                .orderStatus(OrderStatus.SUCCESS)
                .orderItems(List.of(orderItem3))
                .orderCode("order-code-3")
                .build();
        orderRepository.save(order3);

        Review review3 = Review.builder()
                .member(member)
                .product(product)
                .order(order3)
                .comment("세 번째 리뷰입니다")
                .reviewStar(5.0)
                .build();
        reviewRepository.save(review3);

        Long productId = product.getId();
        Pageable pageable = PageRequest.of(0, 2); // 페이지당 2개씩

        // when
        Page<Review> result = reviewRepository.findByProductIdWithPaging(productId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("두 번째 페이지를 조회한다")
    void findByProductIdWithPaging_SecondPage() {
        // given
        // 추가 리뷰 생성 - 각각 다른 Order 사용
        OrderItem orderItem2 = OrderItem.builder()
                .productId(product.getId())
                .build();

        Order order2 = Order.builder()
                .member(member)
                .orderStatus(OrderStatus.SUCCESS)
                .orderItems(List.of(orderItem2))
                .orderCode("order-code-2")
                .build();
        orderRepository.save(order2);

        Review review2 = Review.builder()
                .member(member)
                .product(product)
                .order(order2)
                .comment("두 번째 리뷰입니다")
                .reviewStar(4.0)
                .build();
        reviewRepository.save(review2);

        OrderItem orderItem3 = OrderItem.builder()
                .productId(product.getId())
                .build();

        Order order3 = Order.builder()
                .member(member)
                .orderStatus(OrderStatus.SUCCESS)
                .orderItems(List.of(orderItem3))
                .orderCode("order-code-3")
                .build();
        orderRepository.save(order3);

        Review review3 = Review.builder()
                .member(member)
                .product(product)
                .order(order3)
                .comment("세 번째 리뷰입니다")
                .reviewStar(5.0)
                .build();
        reviewRepository.save(review3);

        Long productId = product.getId();
        Pageable pageable = PageRequest.of(1, 2); // 두 번째 페이지

        // when
        Page<Review> result = reviewRepository.findByProductIdWithPaging(productId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("사용자 ID로 리뷰 조회 시 페이징이 올바르게 작동한다")
    void findByMemberIdWithProduct_Pagination() {
        // given
        // 다른 상품과 리뷰 생성
        Product product2 = Product.builder()
                .member(member)
                .productName("부산 여행")
                .description("아름다운 부산 여행")
                .thumbnailImageUrl("https://example.com/busan.jpg")
                .country(country)
                .build();
        productRepository.save(product2);

        OrderItem orderItem2 = OrderItem.builder()
                .productId(product2.getId())
                .build();

        Order order2 = Order.builder()
                .member(member)
                .orderStatus(OrderStatus.SUCCESS)
                .orderItems(List.of(orderItem2))
                .orderCode("order-code-2")
                .build();
        orderRepository.save(order2);

        Review review2 = Review.builder()
                .member(member)
                .product(product2)
                .order(order2)
                .comment("부산 여행 리뷰입니다")
                .reviewStar(4.0)
                .build();
        reviewRepository.save(review2);

        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 1); // 페이지당 1개씩

        // when
        Page<Review> result = reviewRepository.findByMemberIdWithProduct(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("리뷰가 없는 상품의 경우 빈 페이지를 반환한다")
    void findByProductIdWithPaging_EmptyProduct() {
        // given
        // 리뷰가 없는 새로운 상품 생성
        Product productWithoutReview = Product.builder()
                .member(member)
                .productName("서울 여행")
                .description("아름다운 서울 여행")
                .thumbnailImageUrl("https://example.com/seoul.jpg")
                .country(country)
                .build();
        productRepository.save(productWithoutReview);

        Long productId = productWithoutReview.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Review> result = reviewRepository.findByProductIdWithPaging(productId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("리뷰가 없는 사용자의 경우 빈 페이지를 반환한다")
    void findByMemberIdWithProduct_EmptyMember() {
        // given
        // 리뷰가 없는 새로운 사용자 생성
        Member memberWithoutReview = Member.builder()
                .accountEmail("no-review@test.com")
                .name("리뷰없는유저")
                .nickname("리뷰없는유저")
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(memberWithoutReview);

        Long memberId = memberWithoutReview.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Review> result = reviewRepository.findByMemberIdWithProduct(memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}

