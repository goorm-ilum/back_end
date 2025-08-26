package com.talktrip.talktrip.domain.like.integration;

import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.like.service.LikeService;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.Gender;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
class LikeIntegrationTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CountryRepository countryRepository;

    private Member member1;
    private Member member2;
    private Product product1;
    private Product product2;
    private Country country;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 생성
        country = Country.builder()
                .id(1L)
                .name("대한민국")
                .continent("아시아")
                .build();
        countryRepository.save(country);

        member1 = Member.builder()
                .accountEmail("test1@test.com")
                .name("테스트유저1")
                .nickname("테스터1")
                .gender(Gender.M)
                .birthday(LocalDate.of(1990, 1, 1))
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(member1);

        member2 = Member.builder()
                .accountEmail("test2@test.com")
                .name("테스트유저2")
                .nickname("테스터2")
                .gender(Gender.F)
                .birthday(LocalDate.of(1995, 5, 5))
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(member2);

        product1 = Product.builder()
                .productName("테스트 상품 1")
                .description("테스트 상품 1 설명")
                .thumbnailImageUrl("https://example.com/product1.jpg")
                .member(member1)
                .country(country)
                .build();
        productRepository.save(product1);

        product2 = Product.builder()
                .productName("테스트 상품 2")
                .description("테스트 상품 2 설명")
                .thumbnailImageUrl("https://example.com/product2.jpg")
                .member(member1)
                .country(country)
                .build();
        productRepository.save(product2);
    }

    @Test
    @DisplayName("좋아요를 성공적으로 토글한다 - 처음 좋아요")
    void toggleLike_Success_FirstLike() {
        // given
        Long memberId = member1.getId();
        Long productId = product1.getId();

        // when
        likeService.toggleLike(productId, memberId);

        // then
        assertThat(likeRepository.existsByProductIdAndMemberId(productId, memberId)).isTrue();
        
        // 실제 DB에서 확인
        List<Like> likes = likeRepository.findAll();
        assertThat(likes).hasSize(1);
        assertThat(likes.get(0).getProductId()).isEqualTo(productId);
        assertThat(likes.get(0).getMemberId()).isEqualTo(memberId);
    }

    @Test
    @DisplayName("좋아요를 성공적으로 토글한다 - 좋아요 취소")
    void toggleLike_Success_Unlike() {
        // given
        Long memberId = member1.getId();
        Long productId = product1.getId();
        
        // 먼저 좋아요 추가
        likeService.toggleLike(productId, memberId);
        assertThat(likeRepository.existsByProductIdAndMemberId(productId, memberId)).isTrue();

        // when - 좋아요 취소
        likeService.toggleLike(productId, memberId);

        // then
        assertThat(likeRepository.existsByProductIdAndMemberId(productId, memberId)).isFalse();
        
        // 실제 DB에서 확인
        List<Like> likes = likeRepository.findAll();
        assertThat(likes).isEmpty();
    }

    @Test
    @DisplayName("여러 상품에 좋아요를 토글한다")
    void toggleLike_MultipleProducts() {
        // given
        Long memberId = member1.getId();
        Long productId1 = product1.getId();
        Long productId2 = product2.getId();

        // when
        likeService.toggleLike(productId1, memberId);
        likeService.toggleLike(productId2, memberId);

        // then
        assertThat(likeRepository.existsByProductIdAndMemberId(productId1, memberId)).isTrue();
        assertThat(likeRepository.existsByProductIdAndMemberId(productId2, memberId)).isTrue();
        
        // 실제 DB에서 확인
        List<Like> likes = likeRepository.findAll();
        assertThat(likes).hasSize(2);
        assertThat(likes).extracting("productId").containsExactlyInAnyOrder(productId1, productId2);
        assertThat(likes).extracting("memberId").containsOnly(memberId);
    }

    @Test
    @DisplayName("여러 회원이 같은 상품에 좋아요를 토글한다")
    void toggleLike_MultipleMembers() {
        // given
        Long memberId1 = member1.getId();
        Long memberId2 = member2.getId();
        Long productId = product1.getId();

        // when
        likeService.toggleLike(productId, memberId1);
        likeService.toggleLike(productId, memberId2);

        // then
        assertThat(likeRepository.existsByProductIdAndMemberId(productId, memberId1)).isTrue();
        assertThat(likeRepository.existsByProductIdAndMemberId(productId, memberId2)).isTrue();
        
        // 실제 DB에서 확인
        List<Like> likes = likeRepository.findAll();
        assertThat(likes).hasSize(2);
        assertThat(likes).extracting("productId").containsOnly(productId);
        assertThat(likes).extracting("memberId").containsExactlyInAnyOrder(memberId1, memberId2);
    }

    @Test
    @DisplayName("존재하지 않는 상품에 좋아요 시도 시 예외가 발생한다")
    void toggleLike_ProductNotFound() {
        // given
        Long memberId = member1.getId();
        Long nonExistentProductId = 999L;

        // when & then
        assertThatThrownBy(() -> likeService.toggleLike(nonExistentProductId, memberId))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 좋아요 시도 시 예외가 발생한다")
    void toggleLike_MemberNotFound() {
        // given
        Long nonExistentMemberId = 999L;
        Long productId = product1.getId();

        // when & then
        assertThatThrownBy(() -> likeService.toggleLike(productId, nonExistentMemberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 ID 목록으로 좋아요 상태를 일괄 조회한다")
    void findLikedProductIds_Success() {
        // given
        Long memberId = member1.getId();
        Long productId1 = product1.getId();
        Long productId2 = product2.getId();
        
        // 좋아요 추가
        likeService.toggleLike(productId1, memberId);
        likeService.toggleLike(productId2, memberId);

        // when
        Set<Long> likedProductIds = likeRepository.findLikedProductIds(memberId, List.of(productId1, productId2));

        // then
        assertThat(likedProductIds).hasSize(2);
        assertThat(likedProductIds).containsExactlyInAnyOrder(productId1, productId2);
    }

    @Test
    @DisplayName("상품 ID 목록으로 좋아요 상태 조회 시 좋아요하지 않은 상품은 제외된다")
    void findLikedProductIds_ExcludesNotLiked() {
        // given
        Long memberId = member1.getId();
        Long productId1 = product1.getId();
        Long productId2 = product2.getId();
        
        // product1만 좋아요
        likeService.toggleLike(productId1, memberId);

        // when
        Set<Long> likedProductIds = likeRepository.findLikedProductIds(memberId, List.of(productId1, productId2));

        // then
        assertThat(likedProductIds).hasSize(1);
        assertThat(likedProductIds).containsOnly(productId1);
    }

    @Test
    @DisplayName("상품 ID 목록으로 좋아요 상태 조회 시 빈 리스트를 전달하면 빈 결과를 반환한다")
    void findLikedProductIds_EmptyList() {
        // given
        Long memberId = member1.getId();
        Long productId = product1.getId();
        
        // 좋아요 추가
        likeService.toggleLike(productId, memberId);

        // when
        Set<Long> likedProductIds = likeRepository.findLikedProductIds(memberId, List.of());

        // then
        assertThat(likedProductIds).isEmpty();
    }

    @Test
    @DisplayName("상품 ID 목록으로 좋아요 상태 조회 시 null 리스트를 전달하면 빈 결과를 반환한다")
    void findLikedProductIds_NullList() {
        // given
        Long memberId = member1.getId();
        Long productId = product1.getId();
        
        // 좋아요 추가
        likeService.toggleLike(productId, memberId);

        // when
        Set<Long> likedProductIds = likeRepository.findLikedProductIds(memberId, null);

        // then
        assertThat(likedProductIds).isEmpty();
    }

    @Test
    @DisplayName("좋아요를 삭제한다")
    void deleteLike_Success() {
        // given
        Long memberId = member1.getId();
        Long productId = product1.getId();
        
        // 좋아요 추가
        likeService.toggleLike(productId, memberId);
        assertThat(likeRepository.existsByProductIdAndMemberId(productId, memberId)).isTrue();

        // when
        likeRepository.deleteByProductIdAndMemberId(productId, memberId);

        // then
        assertThat(likeRepository.existsByProductIdAndMemberId(productId, memberId)).isFalse();
        
        // 실제 DB에서 확인
        List<Like> likes = likeRepository.findAll();
        assertThat(likes).isEmpty();
    }

    @Test
    @DisplayName("null 회원 ID로 좋아요 토글 시 예외가 발생한다")
    void toggleLike_NullMemberId() {
        // given
        Long productId = product1.getId();

        // when & then
        assertThatThrownBy(() -> likeService.toggleLike(productId, null))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("null 상품 ID로 좋아요 토글 시 예외가 발생한다")
    void toggleLike_NullProductId() {
        // given
        Long memberId = member1.getId();

        // when & then
        assertThatThrownBy(() -> likeService.toggleLike(null, memberId))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("null 회원 ID로 좋아요 목록 조회 시 예외가 발생한다")
    void getLikedProducts_NullMemberId() {
        // given
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> likeService.getLikedProducts(null, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("좋아요 목록 조회 시 페이징이 올바르게 적용된다")
    void getLikedProducts_WithPaging() {
        // given
        Long memberId = member1.getId();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 1);
        
        // 좋아요 추가
        likeService.toggleLike(product1.getId(), memberId);
        likeService.toggleLike(product2.getId(), memberId);

        // when
        var result = likeService.getLikedProducts(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isFalse();
    }
}
