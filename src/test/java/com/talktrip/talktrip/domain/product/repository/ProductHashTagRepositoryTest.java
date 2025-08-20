package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
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

import java.util.List;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({QuerydslConfig.class, ProductHashTagRepositoryTest.AuditingTestConfig.class})
class ProductHashTagRepositoryTest {

    @Autowired ProductHashTagRepository hashTagRepository;
    @Autowired ProductRepository productRepository;
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

    private void tag(Product p, String t) {
        HashTag h = HashTag.builder().product(p).hashtag(t).build();
        em.persist(h);
    }

    @Test
    @DisplayName("deleteAllByProduct: 대상 상품 해시태그만 제거, 다른 상품 해시태그는 유지")
    void deleteAllByProduct_onlyTarget() {
        Product p1 = product(PRODUCT_NAME_1);
        Product p2 = product(PRODUCT_NAME_2);

        tag(p1, HASHTAG_SEA);
        tag(p1, HASHTAG_FOOD);
        tag(p2, HASHTAG_SEA);

        em.flush();
        em.clear();

        hashTagRepository.deleteAllByProduct(p1);
        em.flush();
        em.clear();

        List<HashTag> remain = em.createQuery("select h from HashTag h", HashTag.class).getResultList();
        assertThat(remain).hasSize(1);
        assertThat(remain.getFirst().getProduct().getId()).isEqualTo(p2.getId());
        assertThat(remain.getFirst().getHashtag()).isEqualTo(HASHTAG_SEA);
    }

    @Test
    @DisplayName("deleteAllByProduct: 해시태그가 없는 상품에 호출해도 예외 없이 통과(경계값)")
    void deleteAllByProduct_noTags_ok() {
        Product p = product(PRODUCT_NAME_3);
        hashTagRepository.deleteAllByProduct(p);
        em.flush();
        em.clear();

        List<HashTag> remain = em.createQuery("select h from HashTag h", HashTag.class).getResultList();
        assertThat(remain).isEmpty();
    }
}
