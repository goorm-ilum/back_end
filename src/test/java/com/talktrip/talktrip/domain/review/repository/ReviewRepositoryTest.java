package com.talktrip.talktrip.domain.review.repository;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDSLTestConfig.class)
@EnableJpaAuditing
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewRepositoryTest {

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

    private Member member;
    private Product product;
    private Order order;
    private OrderItem orderItem;
    private Review review;

    @BeforeEach
    void setUp() {
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
        testEntityManager.persistAndFlush(review);
    }

    @Test
    @DisplayName("리뷰를 성공적으로 저장한다")
    void save_Success() {
        // given
        OrderItem newOrderItem = OrderItem.builder()
                .productId(product.getId())
                .build();

        Order newOrder = Order.builder()
                .member(member)
                .orderStatus(OrderStatus.SUCCESS)
                .orderItems(List.of(newOrderItem))
                .orderCode("order-code-2")
                .build();
        orderRepository.save(newOrder);

        Review newReview = Review.builder()
                .member(member)
                .product(product)
                .order(newOrder)
                .comment("새로운 리뷰입니다")
                .reviewStar(5.0)
                .build();

        // when
        Review savedReview = reviewRepository.save(newReview);

        // then
        assertThat(savedReview.getId()).isNotNull();
        assertThat(savedReview.getComment()).isEqualTo("새로운 리뷰입니다");
        assertThat(savedReview.getMember()).isEqualTo(member);
        assertThat(savedReview.getProduct()).isEqualTo(product);
    }

    @Test
    @DisplayName("ID로 리뷰를 성공적으로 조회한다")
    void findById_Success() {
        // when
        Optional<Review> foundReview = reviewRepository.findById(review.getId());

        // then
        assertThat(foundReview).isPresent();
        assertThat(foundReview.get().getComment()).isEqualTo("좋은 여행이었습니다");
        assertThat(foundReview.get().getMember()).isEqualTo(member);
        assertThat(foundReview.get().getProduct()).isEqualTo(product);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 리뷰 조회 시 빈 Optional을 반환한다")
    void findById_NotFound() {
        // when
        Optional<Review> foundReview = reviewRepository.findById(999L);

        // then
        assertThat(foundReview).isEmpty();
    }

    @Test
    @DisplayName("모든 리뷰를 성공적으로 조회한다")
    void findAll_Success() {
        // given
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

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = reviewRepository.findAll(pageable);

        // then
        assertThat(reviewPage.getContent()).hasSize(2);
        assertThat(reviewPage.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("리뷰를 성공적으로 삭제한다")
    void delete_Success() {
        // when
        reviewRepository.delete(review);

        // then
        Optional<Review> deletedReview = reviewRepository.findById(review.getId());
        assertThat(deletedReview).isEmpty();
    }

    @Test
    @DisplayName("상품 ID로 리뷰를 성공적으로 조회한다")
    void findByProductIdWithPaging_Success() {
        // given
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

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = reviewRepository.findByProductIdWithPaging(product.getId(), pageable);

        // then
        assertThat(reviewPage.getContent()).hasSize(2);
        assertThat(reviewPage.getContent()).allMatch(r -> r.getProduct().getId().equals(product.getId()));
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 리뷰 조회 시 빈 페이지를 반환한다")
    void findByProductIdWithPaging_NoResult() {
        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = reviewRepository.findByProductIdWithPaging(999L, pageable);

        // then
        assertThat(reviewPage.getContent()).isEmpty();
        assertThat(reviewPage.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("사용자 ID로 리뷰를 성공적으로 조회한다")
    void findByMemberIdWithProduct_Success() {
        // given
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
        testEntityManager.persistAndFlush(review2);

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = reviewRepository.findByMemberIdWithProduct(member.getId(), pageable);

        // then
        assertThat(reviewPage.getContent()).hasSize(2);
        assertThat(reviewPage.getContent()).allMatch(r -> r.getMember().getId().equals(member.getId()));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 리뷰 조회 시 빈 페이지를 반환한다")
    void findByMemberIdWithProduct_NoResult() {
        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = reviewRepository.findByMemberIdWithProduct(999L, pageable);

        // then
        assertThat(reviewPage.getContent()).isEmpty();
        assertThat(reviewPage.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("주문 ID로 리뷰 존재 여부를 확인한다")
    void existsByOrderId_Success() {
        // when
        boolean exists = reviewRepository.existsByOrderId(order.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID로 리뷰 존재 여부 확인 시 false를 반환한다")
    void existsByOrderId_NotFound() {
        // when
        boolean exists = reviewRepository.existsByOrderId(999L);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("리뷰가 존재하는지 확인한다")
    void existsById_Success() {
        // when
        boolean exists = reviewRepository.existsById(review.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 ID로 존재 여부 확인 시 false를 반환한다")
    void existsById_NotFound() {
        // when
        boolean exists = reviewRepository.existsById(999L);

        // then
        assertThat(exists).isFalse();
    }
}

