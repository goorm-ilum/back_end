package com.talktrip.talktrip.domain.like.repository;

import com.talktrip.talktrip.domain.like.entity.Like;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({QuerydslConfig.class, LikeRepositoryTest.AuditingTestConfig.class})
class LikeRepositoryTest {

    @Autowired LikeRepository likeRepository;
    @Autowired EntityManager em;

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingTestConfig {}

    private Member Member(String email, MemberRole role) {
        Member m = Member.builder()
                .accountEmail(email)
                .memberRole(role)
                .memberState(MemberState.A)
                .build();
        em.persist(m);
        return m;
    }

    private Product Product(Member member, String name) {
        Product p = Product.builder()
                .productName(name)
                .description("test description")
                .deleted(false)
                .member(member)
                .build();
        em.persist(p);
        return p;
    }

    private void Like(Product product, Member member) {
        Like like = Like.builder()
                .product(product)
                .member(member)
                .build();
        em.persist(like);
    }

    @Test
    @DisplayName("저장된 좋아요가 존재하면 true 반환 테스트")
    void exists_true() {
        // given
        Member member = Member("user@gmail.com", MemberRole.U);
        Product product = Product(member, "P1");
        Like(product, member);

        // when
        boolean exists = likeRepository.existsByProductIdAndMemberId(product.getId(), member.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("저장된 좋아요가 없으면 false 반환 테스트")
    void exists_false() {
        // given
        Member member = Member("user@gmail.com", MemberRole.U);
        Product product = Product(member,"P1");

        // when
        boolean exists = likeRepository.existsByProductIdAndMemberId(product.getId(), member.getId());

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("좋아요 3개 중 첫 페이지 조회 테스트")
    void page_first() {
        // given
        Member member = Member("user@gmail.com", MemberRole.U);
        Member seller = Member("seller@gmail.com", MemberRole.A);
        Product p1 = Product(seller, "P1");
        Product p2 = Product(seller, "P2");
        Product p3 = Product(seller, "P3");
        Like(p1, member);
        Like(p2, member);
        Like(p3, member);

        // when
        Page<Like> page = likeRepository.findByMemberId(member.getId(), PageRequest.of(0, 2));

        // then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("좋아요 3개 중 두 번째 페이지 조회 테스트")
    void page_second() {
        // given
        Member member = Member("user@gmail.com", MemberRole.U);
        Member seller = Member("seller@gmail.com", MemberRole.A);
        Product p1 = Product(seller, "P1");
        Product p2 = Product(seller, "P2");
        Product p3 = Product(seller, "P3");
        Like(p1, member);
        Like(p2, member);
        Like(p3, member);

        // when
        Page<Like> page = likeRepository.findByMemberId(member.getId(), PageRequest.of(1, 2));

        // then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("좋아요가 없을 때 빈 페이지 반환 테스트")
    void empty_boundary() {
        // given
        Member member = Member("user@gmail.com", MemberRole.U);

        // when
        Page<Like> page = likeRepository.findByMemberId(member.getId(), PageRequest.of(0, 10));

        // then
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("저장된 좋아요 삭제 후 존재하지 않음 테스트")
    void delete_success() {
        // given
        Member member = Member("user@gmail.com", MemberRole.U);
        Member seller = Member("seller@gmail.com", MemberRole.A);
        Product product = Product(seller, "P1");
        Like(product, member);

        // when
        likeRepository.deleteByProductIdAndMemberId(product.getId(), member.getId());
        em.flush(); em.clear();

        // then
        boolean exists = likeRepository.existsByProductIdAndMemberId(product.getId(), member.getId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 좋아요 삭제 시 예외 없이 통과 테스트")
    void delete_noop() {
        // when
        likeRepository.deleteByProductIdAndMemberId(1L, 1L);

        // then
        assertThat(likeRepository.count()).isEqualTo(0);
    }
}
