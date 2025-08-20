package com.talktrip.talktrip.domain.review.repository;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.entity.OrderItem;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({QuerydslConfig.class, ReviewRepositoryTest.AuditingTestConfig.class})
class ReviewRepositoryTest {

    @Autowired ReviewRepository reviewRepository;
    @Autowired EntityManager em;

    @TestConfiguration @EnableJpaAuditing
    static class AuditingTestConfig {}

    private Member user() {
        Member m = Member.builder()
                .accountEmail(USER_EMAIL)
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        em.persist(m);
        return m;
    }

    private Member anotherUser() {
        Member m = Member.builder()
                .accountEmail(USER2_EMAIL)
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        em.persist(m);
        return m;
    }

    private Member seller() {
        Member m = Member.builder()
                .accountEmail(SELLER_EMAIL)
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
        em.persist(m);
        return m;
    }

    private Product product(Member s, String name) {
        Product p = Product.builder()
                .member(s)
                .productName(name)
                .description(DESC)
                .deleted(false)
                .build();
        em.persist(p);
        return p;
    }

    private Order order(Member buyer) {
        Order o = Order.builder()
                .member(buyer)
                .orderStatus(OrderStatus.SUCCESS)
                .orderCode(ORDER_CODE_PREFIX + System.nanoTime())
                .build();
        em.persist(o);
        return o;
    }

    private void review(Member buyer, Product p, Order o, float star) {
        OrderItem item = OrderItem.createOrderItem(
                p.getId(), p.getProductName(), p.getThumbnailImageUrl(),
                PRICE_10000, null, null, 0, 0,
                LocalDate.now(), QUANTITY_1, PRICE_10000
        );
        item.setOrder(o);
        o.getOrderItems().add(item);
        em.persist(item);

        Review r = Review.builder()
                .member(buyer)
                .product(p)
                .order(o)
                .comment(DESC)
                .reviewStar(star)
                .build();
        em.persist(r);
    }

    @Test @DisplayName("existsByOrderId: true/false")
    void existsByOrderId() {
        Member buyer = user();
        Member s = seller();
        Product p = product(s, PRODUCT_NAME_1);
        Order o = order(buyer);

        review(buyer, p, o, STAR_4_0);
        assertThat(reviewRepository.existsByOrderId(o.getId())).isTrue();

        Order o2 = order(buyer);
        assertThat(reviewRepository.existsByOrderId(o2.getId())).isFalse();
    }

    @Test @DisplayName("existsByOrderId: 없는 주문 ID는 false")
    void existsByOrderId_negative() {
        assertThat(reviewRepository.existsByOrderId(NON_EXIST_ORDER_ID)).isFalse();
    }

    @Test @DisplayName("findByMemberId 페이징(총3, page0 size2 -> content size=2)")
    void findByMemberId_paging() {
        Member buyer = user();
        Member s = seller();
        Product p1 = product(s, PRODUCT_NAME_1);
        Product p2 = product(s, PRODUCT_NAME_2);
        Product p3 = product(s, PRODUCT_NAME_3);
        Order o1 = order(buyer);
        Order o2 = order(buyer);
        Order o3 = order(buyer);

        review(buyer, p1, o1, STAR_5_0);
        review(buyer, p2, o2, STAR_3_0);
        review(buyer, p3, o3, STAR_4_0);

        Page<Review> page = reviewRepository.findByMemberId(buyer.getId(), PageRequest.of(PAGE_0, SIZE_2));
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(SIZE_2);
        assertThat(page.getNumber()).isEqualTo(PAGE_0);
        assertThat(page.getSize()).isEqualTo(SIZE_2);
    }

    @Test @DisplayName("findByMemberId: 다른 회원 리뷰가 섞이지 않음")
    void findByMemberId_isolation() {
        Member m1 = user();
        Member m2 = anotherUser();
        Member s = seller();
        Product p = product(s, PRODUCT_NAME_1);
        Order o1 = order(m1);
        Order o2 = order(m2);

        review(m1, p, o1, STAR_5_0);
        review(m2, p, o2, STAR_3_0);

        Page<Review> page = reviewRepository.findByMemberId(m1.getId(), PageRequest.of(PAGE_0, SIZE_10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getMember().getId()).isEqualTo(m1.getId());
    }

    @Test @DisplayName("findByProductId 페이징(총2, page0 size1 -> content size=1)")
    void findByProductId_paging() {
        Member buyer = user();
        Member s = seller();
        Product p = product(s, PRODUCT_NAME_1);
        Order o1 = order(buyer);
        Order o2 = order(buyer);

        review(buyer, p, o1, STAR_5_0);
        review(buyer, p, o2, STAR_2_0);

        Page<Review> page = reviewRepository.findByProductId(p.getId(), PageRequest.of(PAGE_0, SIZE_1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(SIZE_1);
        assertThat(page.getNumber()).isEqualTo(PAGE_0);
        assertThat(page.getSize()).isEqualTo(SIZE_1);
    }

    @Test @DisplayName("findByProductId: 초과 페이지는 빈 페이지")
    void findByProductId_overflow() {
        Member buyer = user();
        Member s = seller();
        Product p = product(s, PRODUCT_NAME_1);

        review(buyer, p, order(buyer), STAR_5_0);
        review(buyer, p, order(buyer), STAR_4_0);
        review(buyer, p, order(buyer), STAR_3_0);

        Page<Review> page = reviewRepository.findByProductId(p.getId(), PageRequest.of(PAGE_2, SIZE_2));
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test @DisplayName("findByProductId: updatedAt DESC 정렬 준수")
    void findByProductId_sort_desc_by_updatedAt() {
        Member buyer = user();
        Member s = seller();
        Product p = product(s, PRODUCT_NAME_1);

        review(buyer, p, order(buyer), STAR_3_0);
        review(buyer, p, order(buyer), STAR_5_0);

        Page<Review> page = reviewRepository.findByProductId(
                p.getId(), PageRequest.of(PAGE_0, SIZE_10, Sort.by(Sort.Direction.DESC, SORT_UPDATED_AT)));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getUpdatedAt())
                .isAfterOrEqualTo(page.getContent().get(1).getUpdatedAt());
    }

    @Test @DisplayName("@Query findByProductIdIncludingDeleted 동작")
    void findByProductIdIncludingDeleted() {
        Member buyer = user();
        Member s = seller();
        Product p = product(s, PRODUCT_NAME_1);
        Order o = order(buyer);

        review(buyer, p, o, STAR_4_0);

        List<Review> list = reviewRepository.findByProductIdIncludingDeleted(p.getId());
        assertThat(list).isNotEmpty();
        assertThat(list.getFirst().getProduct().getId()).isEqualTo(p.getId());
    }

    @Test @DisplayName("@Query findByProductIdIncludingDeleted: 다른 상품 리뷰 미포함")
    void includingDeleted_isolation_by_product() {
        Member buyer = user();
        Member s = seller();
        Product p1 = product(s, PRODUCT_NAME_1);
        Product p2 = product(s, PRODUCT_NAME_2);

        review(buyer, p1, order(buyer), STAR_4_0);
        review(buyer, p2, order(buyer), STAR_2_0);

        List<Review> list = reviewRepository.findByProductIdIncludingDeleted(p1.getId());
        assertThat(list).isNotEmpty();
        assertThat(list).allMatch(r -> r.getProduct().getId().equals(p1.getId()));
    }
}
