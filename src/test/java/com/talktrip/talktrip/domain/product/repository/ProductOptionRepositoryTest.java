package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({QuerydslConfig.class, ProductOptionRepositoryTest.AuditingTestConfig.class})
class ProductOptionRepositoryTest {

    @Autowired ProductOptionRepository productOptionRepository;
    @Autowired EntityManager em;

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingTestConfig { }

    private Member cachedSeller;

    private Member seller() {
        if (cachedSeller != null) return cachedSeller;
        Member m = Member.builder()
                .accountEmail(SELLER_EMAIL)   // 고정 상수여도 OK (동일 테스트 내 1회만 persist)
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
        em.persist(m);
        cachedSeller = m;
        return m;
    }

    private Product product(String name) {
        Product p = Product.builder()
                .member(seller())
                .productName(name)
                .description(DESC)
                .deleted(false)
                .build();
        em.persist(p);
        return p;
    }

    private void option(Product p, String name) {
        ProductOption o = ProductOption.builder()
                .product(p)
                .optionName(name)
                .startDate(LocalDate.now())
                .stock(STOCK_3)
                .price(PRICE_10000)
                .discountPrice(DISC_9000)
                .build();
        em.persist(o);
    }

    @Test
    @DisplayName("deleteAllByProduct: 대상 상품 옵션만 삭제, 다른 상품 옵션 유지")
    void deleteAllByProduct_onlyTarget() {
        Product p1 = product(PRODUCT_NAME_1);
        Product p2 = product(PRODUCT_NAME_2);

        option(p1, OPTION_NAME);
        option(p2, OPTION_NAME_2);

        em.flush();
        em.clear();

        productOptionRepository.deleteAllByProduct(p1);
        em.flush();
        em.clear();

        List<ProductOption> remain = em.createQuery("select o from ProductOption o", ProductOption.class).getResultList();
        assertThat(remain).hasSize(1);
        assertThat(remain.getFirst().getProduct().getId()).isEqualTo(p2.getId());
    }

    @Test
    @DisplayName("deleteAllByProduct: 옵션이 없는 상품에 호출해도 예외 없이 통과(경계값)")
    void deleteAllByProduct_noOptions_ok() {
        Product p = product(PRODUCT_NAME_3);
        productOptionRepository.deleteAllByProduct(p);
        em.flush();
        em.clear();

        List<ProductOption> remain = em.createQuery("select o from ProductOption o", ProductOption.class).getResultList();
        assertThat(remain).isEmpty();
    }
}
