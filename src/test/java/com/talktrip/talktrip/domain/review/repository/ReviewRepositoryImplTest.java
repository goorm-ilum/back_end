package com.talktrip.talktrip.domain.review.repository;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.order.entity.Order;
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
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(QueryDSLTestConfig.class)
@EnableJpaAuditing
@ActiveProfiles("test")
class ReviewRepositoryImplTest {

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private TestEntityManager entityManager;

    private Member member;
    private Product product;
    private Order order1;
    private Order order2;
    private Review reviewHigh;
    private Review reviewLow;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .accountEmail("repo-test@test.com")
                .name("tester")
                .nickname("tester")
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(member);

        Country kr = Country.builder().id(1L).name("대한민국").build();
        entityManager.persist(kr);
        product = Product.builder()
                .member(member)
                .country(kr)
                .productName("테스트 상품")
                .description("설명")
                .thumbnailImageUrl(null)
                .build();
        productRepository.save(product);

        order1 = Order.builder()
                .member(member)
                .orderCode("REV-ORD-1")
                .orderDate(LocalDate.now())
                .orderStatus(OrderStatus.SUCCESS)
                .totalPrice(10000)
                .build();
        orderRepository.save(order1);

        order2 = Order.builder()
                .member(member)
                .orderCode("REV-ORD-2")
                .orderDate(LocalDate.now())
                .orderStatus(OrderStatus.SUCCESS)
                .totalPrice(20000)
                .build();
        orderRepository.save(order2);

        reviewHigh = Review.builder()
                .member(member)
                .product(product)
                .order(order1)
                .reviewStar(5.0)
                .comment("아주 좋음")
                .build();
        reviewRepository.save(reviewHigh);

        reviewLow = Review.builder()
                .member(member)
                .product(product)
                .order(order2)
                .reviewStar(3.0)
                .comment("보통")
                .build();
        reviewRepository.save(reviewLow);
    }

    // findByProductIdWithPaging

    @Test
    @DisplayName("상품 리뷰 페이징 - 기본 정렬(updatedAt DESC)")
    void findByProductId_DefaultSort() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> page = reviewRepository.findByProductIdWithPaging(product.getId(), pageable);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("상품 리뷰 페이징 - reviewStar ASC/DESC")
    void findByProductId_SortByReviewStar() {
        Pageable asc = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("reviewStar")));
        Pageable desc = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("reviewStar")));

        Page<Review> ascPage = reviewRepository.findByProductIdWithPaging(product.getId(), asc);
        Page<Review> descPage = reviewRepository.findByProductIdWithPaging(product.getId(), desc);

        assertThat(ascPage.getContent().get(0).getReviewStar()).isLessThanOrEqualTo(
                ascPage.getContent().get(ascPage.getContent().size() - 1).getReviewStar());
        assertThat(descPage.getContent().get(0).getReviewStar()).isGreaterThanOrEqualTo(
                descPage.getContent().get(descPage.getContent().size() - 1).getReviewStar());
    }

    @Test
    @DisplayName("상품 리뷰 페이징 - updatedAt ASC/DESC")
    void findByProductId_SortByUpdatedAt() {
        Pageable asc = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("updatedAt")));
        Pageable desc = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("updatedAt")));
        Page<Review> ascPage = reviewRepository.findByProductIdWithPaging(product.getId(), asc);
        Page<Review> descPage = reviewRepository.findByProductIdWithPaging(product.getId(), desc);
        assertThat(ascPage.getContent()).isNotEmpty();
        assertThat(descPage.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("상품 리뷰 페이징 - 미지원 정렬 시 400")
    void findByProductId_UnsupportedSort_Throws400() {
        Pageable bad = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("notExists")));
        assertThatThrownBy(() -> reviewRepository.findByProductIdWithPaging(product.getId(), bad))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Unsupported sort property");
    }

    @Test
    @DisplayName("상품 리뷰 페이징 - 결과 없음 total=0")
    void findByProductId_Empty_TotalZero() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> page = reviewRepository.findByProductIdWithPaging(999999L, pageable);
        assertThat(page.getTotalElements()).isEqualTo(0);
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("내 리뷰 페이징 - 기본 정렬(updatedAt DESC) 및 fetchJoin 확인")
    void findByMemberId_DefaultSort() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> page = reviewRepository.findByMemberIdWithProduct(member.getId(), pageable);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getProduct()).isNotNull();
    }

    @Test
    @DisplayName("내 리뷰 페이징 - reviewStar ASC/DESC")
    void findByMemberId_SortByReviewStar() {
        Pageable asc = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("reviewStar")));
        Pageable desc = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("reviewStar")));
        Page<Review> ascPage = reviewRepository.findByMemberIdWithProduct(member.getId(), asc);
        Page<Review> descPage = reviewRepository.findByMemberIdWithProduct(member.getId(), desc);
        assertThat(ascPage.getContent()).isNotEmpty();
        assertThat(descPage.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("내 리뷰 페이징 - updatedAt ASC/DESC")
    void findByMemberId_SortByUpdatedAt() {
        Pageable asc = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("updatedAt")));
        Pageable desc = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("updatedAt")));
        Page<Review> ascPage = reviewRepository.findByMemberIdWithProduct(member.getId(), asc);
        Page<Review> descPage = reviewRepository.findByMemberIdWithProduct(member.getId(), desc);
        assertThat(ascPage.getContent()).isNotEmpty();
        assertThat(descPage.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("내 리뷰 페이징 - 미지원 정렬 시 400")
    void findByMemberId_UnsupportedSort_Throws400() {
        Pageable bad = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("notExists")));
        assertThatThrownBy(() -> reviewRepository.findByMemberIdWithProduct(member.getId(), bad))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Unsupported sort property");
    }

    @Test
    @DisplayName("내 리뷰 페이징 - 결과 없음 total=0")
    void findByMemberId_Empty_TotalZero() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> page = reviewRepository.findByMemberIdWithProduct(999999L, pageable);
        assertThat(page.getTotalElements()).isEqualTo(0);
        assertThat(page.getContent()).isEmpty();
    }
}

