package com.talktrip.talktrip.domain.like.repository;

import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
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

import java.util.List;
import java.util.Set;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({QuerydslConfig.class, LikeRepositoryTest.AuditingTestConfig.class})
class LikeRepositoryTest {

    @Autowired LikeRepository likeRepository;
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

    private void like(Member member, Product product) {
        Like l = Like.builder().member(member).product(product).build();
        em.persist(l);
    }

    @AfterEach
    void clearPersistence() { em.flush(); em.clear(); }

    @Test @DisplayName("existsByProductIdAndMemberId: 저장된 좋아요가 있으면 true")
    void exists_true() {
        Member u = user();
        Member s = seller();
        Product p = product(s, PRODUCT_NAME_1);
        like(u, p);

        boolean exists = likeRepository.existsByProductIdAndMemberId(p.getId(), u.getId());
        assertThat(exists).isTrue();
    }

    @Test @DisplayName("existsByProductIdAndMemberId: 저장된 좋아요가 없으면 false")
    void exists_false() {
        Member u = user();
        Member s = seller();
        Product p = product(s, PRODUCT_NAME_1);

        boolean exists = likeRepository.existsByProductIdAndMemberId(p.getId(), u.getId());
        assertThat(exists).isFalse();
    }

    @Test @DisplayName("findByMemberId 페이징(총3, page0 size2 -> content size=2)")
    void findByMemberId_page_first() {
        Member u = user();
        Member s = seller();
        Product p1 = product(s, PRODUCT_NAME_1);
        Product p2 = product(s, PRODUCT_NAME_2);
        Product p3 = product(s, PRODUCT_NAME_3);
        like(u, p1); like(u, p2); like(u, p3);

        Page<Like> page = likeRepository.findByMemberId(u.getId(), PageRequest.of(PAGE_0, SIZE_2));
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(SIZE_2);
        assertThat(page.getNumber()).isEqualTo(PAGE_0);
        assertThat(page.getSize()).isEqualTo(SIZE_2);
    }

    @Test @DisplayName("findByMemberId 페이징(총3, page1 size2 -> content size=1)")
    void findByMemberId_page_second() {
        Member u = user();
        Member s = seller();
        Product p1 = product(s, PRODUCT_NAME_1);
        Product p2 = product(s, PRODUCT_NAME_2);
        Product p3 = product(s, PRODUCT_NAME_3);
        like(u, p1); like(u, p2); like(u, p3);

        Page<Like> page = likeRepository.findByMemberId(u.getId(), PageRequest.of(PAGE_1, SIZE_2));
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getNumber()).isEqualTo(PAGE_1);
        assertThat(page.getSize()).isEqualTo(SIZE_2);
    }

    @Test @DisplayName("findByMemberId: createdAt DESC 정렬 준수")
    void findByMemberId_sort_desc() {
        Member u = user();
        Member s = seller();
        Product p1 = product(s, PRODUCT_NAME_1);
        Product p2 = product(s, PRODUCT_NAME_2);
        like(u, p1); like(u, p2);

        Page<Like> page = likeRepository.findByMemberId(
                u.getId(), PageRequest.of(PAGE_0, SIZE_10, Sort.by("createdAt").descending()));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getCreatedAt())
                .isAfterOrEqualTo(page.getContent().get(1).getCreatedAt());
    }

    @Test @DisplayName("findByMemberId: 다른 회원 좋아요는 포함되지 않음")
    void findByMemberId_isolated_by_member() {
        Member u1 = user();
        Member s = seller();
        Member u2 = Member.builder()
                .accountEmail(USER2_EMAIL)
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        em.persist(u2);

        Product p1 = product(s, PRODUCT_NAME_1);
        Product p2 = product(s, PRODUCT_NAME_2);
        like(u1, p1);
        like(u2, p2);

        Page<Like> page = likeRepository.findByMemberId(u1.getId(), PageRequest.of(PAGE_0, SIZE_10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getMember().getId()).isEqualTo(u1.getId());
    }

    @Test @DisplayName("findByMemberId: 초과 페이지는 빈 페이지 반환")
    void findByMemberId_page_overflow() {
        Member u = user();
        Member s = seller();
        like(u, product(s, PRODUCT_NAME_1));
        like(u, product(s, PRODUCT_NAME_2));
        like(u, product(s, PRODUCT_NAME_3));

        Page<Like> page = likeRepository.findByMemberId(u.getId(), PageRequest.of(PAGE_2, SIZE_2));
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test @DisplayName("좋아요가 없을 때 빈 페이지 반환")
    void empty_boundary() {
        Member u = user();

        Page<Like> page = likeRepository.findByMemberId(u.getId(), PageRequest.of(PAGE_0, SIZE_10));
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getNumber()).isEqualTo(PAGE_0);
        assertThat(page.getSize()).isEqualTo(SIZE_10);
    }

    @Test @DisplayName("deleteByProductIdAndMemberId: 저장된 좋아요 삭제 후 존재하지 않음")
    void delete_success() {
        Member u = user();
        Member s = seller();
        Product p = product(s, PRODUCT_NAME_1);
        like(u, p);

        likeRepository.deleteByProductIdAndMemberId(p.getId(), u.getId());
        em.flush(); em.clear();

        boolean exists = likeRepository.existsByProductIdAndMemberId(p.getId(), u.getId());
        assertThat(exists).isFalse();
    }

    @Test @DisplayName("deleteByProductIdAndMemberId: 존재하지 않는 좋아요 삭제 시 예외 없이 통과")
    void delete_noop() {
        likeRepository.deleteByProductIdAndMemberId(1L, 1L);
        assertThat(likeRepository.count()).isEqualTo(0);
    }

    @Test @DisplayName("findLikedProductIdsRaw: 주어진 productIds 교집합만 반환")
    void findLikedProductIdsRaw_intersection() {
        Member u = user();
        Member s = seller();
        Product p1 = product(s, PRODUCT_NAME_1);
        Product p2 = product(s, PRODUCT_NAME_2);
        Product p3 = product(s, PRODUCT_NAME_3);

        like(u, p1);
        like(u, p3);

        List<Long> raw = likeRepository.findLikedProductIdsRaw(u.getId(), List.of(p1.getId(), p2.getId()));

        assertThat(raw).containsExactlyInAnyOrder(p1.getId());
        assertThat(raw).doesNotContain(p2.getId());
    }

    @Test @DisplayName("findLikedProductIds: 기본 메서드가 Set 으로 변환하고 null/빈 리스트 방어")
    void findLikedProductIds_defaults_and_defensive() {
        Member u = user();
        Member s = seller();
        Product p1 = product(s, PRODUCT_NAME_1);
        Product p2 = product(s, PRODUCT_NAME_2);
        like(u, p1);

        Set<Long> liked = likeRepository.findLikedProductIds(u.getId(), List.of(p1.getId(), p2.getId()));
        assertThat(liked).containsExactlyInAnyOrder(p1.getId());

        assertThat(likeRepository.findLikedProductIds(null, List.of(p1.getId()))).isEmpty();

        assertThat(likeRepository.findLikedProductIds(u.getId(), null)).isEmpty();
        assertThat(likeRepository.findLikedProductIds(u.getId(), List.of())).isEmpty();
    }

    @Test @DisplayName("findLikedProductIds: 좋아요가 전혀 없으면 빈 Set 반환")
    void findLikedProductIds_empty_when_no_likes() {
        Member u = user();
        Member s = seller();
        Product p1 = product(s, PRODUCT_NAME_1);
        Product p2 = product(s, PRODUCT_NAME_2);

        Set<Long> liked = likeRepository.findLikedProductIds(u.getId(), List.of(p1.getId(), p2.getId()));
        assertThat(liked).isEmpty();
    }
}
