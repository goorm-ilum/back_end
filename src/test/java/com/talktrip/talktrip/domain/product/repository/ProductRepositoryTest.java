package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.product.entity.Product;
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

import java.time.LocalDateTime;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({QuerydslConfig.class, ProductRepositoryTest.AuditingTestConfig.class})
class ProductRepositoryTest {

    @Autowired ProductRepository productRepository;
    @Autowired EntityManager em;

    @TestConfiguration @EnableJpaAuditing
    static class AuditingTestConfig { }

    private Member seller() {
        Member seller = Member.builder()
                .name(SELLER_NAME)
                .accountEmail(SELLER_EMAIL)
                .phoneNum(PHONE_NUMBER)
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
        em.persist(seller);
        return seller;
    }

    private Member other() {
        Member other = Member.builder()
                .name("other")
                .accountEmail(USER_EMAIL)
                .phoneNum(PHONE_NUMBER)
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        em.persist(other);
        return other;
    }



    private Product product(Member seller, boolean deleted) {
        return Product.builder()
                .member(seller)
                .productName(PRODUCT_NAME_1)
                .description(DESC)
                .thumbnailImageUrl(THUMBNAIL_URL)
                .deleted(deleted)
                .deletedAt(deleted ? LocalDateTime.now() : null)
                .build();
    }

    @Test
    @DisplayName("@Where로 삭제 제외: findById는 deleted=false만 반환")
    void findById_excludesDeleted() {
        Member seller = seller();
        Product active = product(seller, false);
        Product deleted = product(seller, true);

        productRepository.save(active);
        productRepository.save(deleted);

        // 1차 캐시를 비워서 실제 쿼리가 나가게 함
        em.flush();
        em.clear();

        assertThat(productRepository.findById(active.getId())).isPresent();
        assertThat(productRepository.findById(deleted.getId())).isEmpty(); // @Where 적용
    }

    @Test
    @DisplayName("findByIdIncludingDeleted: 삭제 포함")
    void findByIdIncludingDeleted() {
        Member seller = seller();
        Product del = product(seller, true);
        productRepository.save(del);

        em.flush();
        em.clear();

        assertThat(productRepository.findByIdIncludingDeleted(del.getId())).isPresent();
    }

    @Test
    @DisplayName("findByIdAndMemberIdIncludingDeleted: 소유자 일치 시 반환, 불일치 시 빈 값")
    void ownerMatch() {
        Member owner = seller();
        Member other = other();

        Product p = product(owner, false);
        productRepository.save(p);

        em.flush();
        em.clear();

        assertThat(productRepository.findByIdAndMemberIdIncludingDeleted(p.getId(), owner.getId())).isPresent();
        assertThat(productRepository.findByIdAndMemberIdIncludingDeleted(p.getId(), other.getId())).isEmpty();
    }
}
