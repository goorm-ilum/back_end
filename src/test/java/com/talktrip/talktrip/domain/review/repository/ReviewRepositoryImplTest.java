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
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({QuerydslConfig.class, ReviewRepositoryImplTest.AuditingTestConfig.class})
class ReviewRepositoryImplTest {

    @Autowired EntityManager em;
    @Autowired ReviewRepository reviewRepository;

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingTestConfig {}

    private Member user() {
        Member m = Member.builder()
                .accountEmail(USER_EMAIL)
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .name(USER_NAME)
                .phoneNum(PHONE_NUMBER)
                .build();
        em.persist(m);
        return m;
    }

    private Member seller() {
        Member m = Member.builder()
                .accountEmail(SELLER_EMAIL)
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .name(SELLER_NAME)
                .phoneNum(PHONE_NUMBER)
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
                .comment("c")
                .reviewStar(star)
                .build();
        em.persist(r);
    }

    private void flushClear() {
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("기본: 여러 상품의 평균 별점 배치 조회 (리뷰 없는 상품은 결과에 포함되지 않음)")
    void basic_batch_avg() {
        Member s = seller();
        Member u = user();

        Product p1 = product(s, PRODUCT_NAME_1);
        Product p2 = product(s, PRODUCT_NAME_2);
        Product p3 = product(s, PRODUCT_NAME_3); // 리뷰 없음

        review(u, p1, order(u), STAR_4_0);
        review(u, p1, order(u), STAR_2_0);

        review(u, p2, order(u), STAR_5_0);

        flushClear();

        Map<Long, Double> map = reviewRepository.fetchAvgStarsByProductIds(
                List.of(p1.getId(), p2.getId(), p3.getId())
        );

        assertThat(map).containsEntry(p1.getId(), (double) AVG_3_0);
        assertThat(map).containsEntry(p2.getId(), (double) AVG_5_0);
        // 리뷰가 전혀 없으면 group by 결과가 없으므로 키 자체가 없음
        assertThat(map).doesNotContainKey(p3.getId());
    }

    @Test
    @DisplayName("소수점 평균 1자리 반올림: (4.5, 3.0, 4.0) → 3.8")
    void decimal_precision_rounded_to_1dp() {
        Member s = seller();
        Member u = user();
        Product p = product(s, PRODUCT_NAME_1);

        review(u, p, order(u), STAR_4_5);
        review(u, p, order(u), STAR_3_0);
        review(u, p, order(u), STAR_4_0);

        flushClear();

        Map<Long, Double> map = reviewRepository.fetchAvgStarsByProductIds(List.of(p.getId()));

        double raw = (STAR_4_5 + STAR_3_0 + STAR_4_0) / 3.0;
        double expectedRounded = Math.round(raw * 10.0) / 10.0;

        assertThat(map).containsKey(p.getId());
        assertThat(map.get(p.getId())).isEqualTo(expectedRounded);
    }

    @Test
    @DisplayName("productIds=null → 빈 Map")
    void null_ids_returns_empty() {
        Map<Long, Double> map = reviewRepository.fetchAvgStarsByProductIds(null);
        assertThat(map).isEmpty();
    }

    @Test
    @DisplayName("productIds=빈 목록 → 빈 Map")
    void empty_ids_returns_empty() {
        Map<Long, Double> map = reviewRepository.fetchAvgStarsByProductIds(List.of());
        assertThat(map).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 ID가 포함되어도 무시 (해당 키 없음)")
    void non_existing_ids_ignored() {
        Member s = seller();
        Member u = user();
        Product p = product(s, PRODUCT_NAME_1);

        review(u, p, order(u), STAR_4_0);
        flushClear();

        Map<Long, Double> map = reviewRepository.fetchAvgStarsByProductIds(
                List.of(p.getId(), OTHER_PRODUCT_ID) // OTHER_PRODUCT_ID는 DB에 없음
        );

        assertThat(map).containsKey(p.getId());
        assertThat(map).doesNotContainKey(OTHER_PRODUCT_ID);
    }

    @Test
    @DisplayName("입력 productIds에 중복이 있어도 결과는 각 상품당 1개 key만")
    void duplicated_ids_input_produces_single_key() {
        Member s = seller();
        Member u = user();
        Product p = product(s, PRODUCT_NAME_2);

        review(u, p, order(u), STAR_5_0);
        flushClear();

        Map<Long, Double> map = reviewRepository.fetchAvgStarsByProductIds(
                List.of(p.getId(), p.getId(), p.getId())
        );

        assertThat(map.keySet().stream().filter(id -> id.equals(p.getId())).count()).isEqualTo(1);
        assertThat(map.get(p.getId())).isEqualTo(STAR_5_0);
    }

    @Test
    @DisplayName("요청한 ID 외의 상품 리뷰는 집계에 포함되지 않음")
    void only_requested_ids_are_aggregated() {
        Member s = seller();
        Member u = user();

        Product target = product(s, PRODUCT_NAME_1);
        Product other = product(s, PRODUCT_NAME_2);

        review(u, target, order(u), STAR_4_0);
        review(u, other, order(u), STAR_5_0);
        flushClear();

        Map<Long, Double> map = reviewRepository.fetchAvgStarsByProductIds(List.of(target.getId()));
        assertThat(map).containsKey(target.getId());
        assertThat(map).doesNotContainKey(other.getId());
    }

    @Test
    @DisplayName("여러 상품 + 리뷰 편차가 커도 그룹별 평균 정확")
    void many_products_varied_reviews() {
        Member s = seller();
        Member u = user();

        Product p1 = product(s, "A");
        Product p2 = product(s, "B");
        Product p3 = product(s, "C");

        review(u, p1, order(u), STAR_3_0);

        review(u, p2, order(u), STAR_2_0);
        review(u, p2, order(u), STAR_5_0);

        review(u, p3, order(u), STAR_4_5);
        review(u, p3, order(u), STAR_4_0);
        review(u, p3, order(u), STAR_5_0);

        flushClear();

        Map<Long, Double> map = reviewRepository.fetchAvgStarsByProductIds(
                List.of(p1.getId(), p2.getId(), p3.getId())
        );

        double expectedP1 = Math.round((STAR_3_0) * 10.0) / 10.0;
        double expectedP2 = Math.round(((STAR_2_0 + STAR_5_0) / 2.0) * 10.0) / 10.0;
        double expectedP3 = Math.round(((STAR_4_5 + STAR_4_0 + STAR_5_0) / 3.0) * 10.0) / 10.0;

        assertThat(map).containsEntry(p1.getId(), expectedP1);
        assertThat(map).containsEntry(p2.getId(), expectedP2);
        assertThat(map).containsEntry(p3.getId(), expectedP3);
    }

    @Test
    @DisplayName("삭제된(soft delete) 상품의 리뷰도 평균 집계에 포함")
    void deleted_product_reviews_are_included_in_avg() {
        Member s = seller();
        Member u = user();
        Product p = product(s, PRODUCT_NAME_1);

        review(u, p, order(u), STAR_4_0);
        review(u, p, order(u), STAR_2_0);

        p.markDeleted();

        flushClear();

        Map<Long, Double> map = reviewRepository.fetchAvgStarsByProductIds(List.of(p.getId()));

        assertThat(map).containsEntry(p.getId(), (double) AVG_3_0);
    }

}
