package com.talktrip.talktrip.domain.like.repository;

import com.talktrip.talktrip.domain.like.entity.Like;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.talktrip.talktrip.global.config.QueryDSLTestConfig;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@Import(QueryDSLTestConfig.class)
@EnableJpaAuditing
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LikeRepositoryTest {

    @Autowired
    private LikeRepository likeRepository;

    private Like like1;
    private Like like2;
    private Like like3;

    @BeforeEach
    void setUp() {
        like1 = Like.builder()
                .productId(1L)
                .memberId(1L)
                .build();

        like2 = Like.builder()
                .productId(2L)
                .memberId(1L)
                .build();

        like3 = Like.builder()
                .productId(1L)
                .memberId(2L)
                .build();

        likeRepository.saveAll(List.of(like1, like2, like3));
    }

    @Test
    @DisplayName("좋아요가 존재하는 경우 true를 반환한다")
    void existsByProductIdAndMemberId_WhenLikeExists_ReturnsTrue() {
        // given
        Long productId = 1L;
        Long memberId = 1L;

        // when
        boolean exists = likeRepository.existsByProductIdAndMemberId(productId, memberId);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("좋아요가 존재하지 않는 경우 false를 반환한다")
    void existsByProductIdAndMemberId_WhenLikeNotExists_ReturnsFalse() {
        // given
        Long productId = 999L;
        Long memberId = 999L;

        // when
        boolean exists = likeRepository.existsByProductIdAndMemberId(productId, memberId);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("productId가 null인 경우 false를 반환한다")
    void existsByProductIdAndMemberId_WhenProductIdIsNull_ReturnsFalse() {
        // given
        Long memberId = 1L;

        // when
        boolean exists = likeRepository.existsByProductIdAndMemberId(null, memberId);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("memberId가 null인 경우 false를 반환한다")
    void existsByProductIdAndMemberId_WhenMemberIdIsNull_ReturnsFalse() {
        // given
        Long productId = 1L;

        // when
        boolean exists = likeRepository.existsByProductIdAndMemberId(productId, null);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("좋아요한 상품 ID 목록을 조회할 수 있다")
    void findLikedProductIds_ReturnsLikedProductIds() {
        // given
        Long memberId = 1L;
        List<Long> productIds = List.of(1L, 2L, 999L);

        // when
        Set<Long> result = likeRepository.findLikedProductIds(memberId, productIds);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).contains(1L, 2L);
        assertThat(result).doesNotContain(999L);
    }

    @Test
    @DisplayName("좋아요한 상품이 없는 경우 빈 값을 반환한다")
    void findLikedProductIds_WhenNoLikes_ReturnsEmptySet() {
        // given
        Long memberId = 999L;
        List<Long> productIds = List.of(1L, 2L);

        // when
        Set<Long> result = likeRepository.findLikedProductIds(memberId, productIds);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("memberId가 null인 경우 빈 값을 반환한다")
    void findLikedProductIds_WhenMemberIdIsNull_ReturnsEmptySet() {
        // given
        List<Long> productIds = List.of(1L, 2L);

        // when
        Set<Long> result = likeRepository.findLikedProductIds(null, productIds);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("productIds가 null인 경우 빈 값을 반환한다")
    void findLikedProductIds_WhenProductIdsIsNull_ReturnsEmptySet() {
        // given
        Long memberId = 1L;

        // when
        Set<Long> result = likeRepository.findLikedProductIds(memberId, null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("productIds가 빈 리스트인 경우 빈 값을 반환한다")
    void findLikedProductIds_WhenProductIdsIsEmpty_ReturnsEmptySet() {
        // given
        Long memberId = 1L;
        List<Long> productIds = List.of();

        // when
        Set<Long> result = likeRepository.findLikedProductIds(memberId, productIds);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("좋아요를 저장할 수 있다")
    void save_NewLike_SavesSuccessfully() {
        // given
        Like newLike = Like.builder()
                .productId(999L)
                .memberId(999L)
                .build();

        // when
        Like savedLike = likeRepository.save(newLike);

        // then
        assertThat(savedLike.getId()).isNotNull();
        assertThat(savedLike.getProductId()).isEqualTo(999L);
        assertThat(savedLike.getMemberId()).isEqualTo(999L);
        assertThat(savedLike.getCreatedAt()).isNotNull();
        assertThat(savedLike.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("좋아요를 삭제할 수 있다")
    void deleteByProductIdAndMemberId_DeletesLike() {
        // given
        Long productId = 1L;
        Long memberId = 1L;

        // when
        likeRepository.deleteByProductIdAndMemberId(productId, memberId);

        // then
        boolean exists = likeRepository.existsByProductIdAndMemberId(productId, memberId);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 좋아요를 삭제해도 예외가 발생하지 않는다")
    void deleteByProductIdAndMemberId_WhenLikeNotExists_DoesNotThrowException() {
        // given
        Long productId = 999L;
        Long memberId = 999L;

        // when & then
        assertThatCode(() -> likeRepository.deleteByProductIdAndMemberId(productId, memberId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("모든 좋아요를 조회할 수 있다")
    void findAll_ReturnsAllLikes() {
        // when
        List<Like> allLikes = likeRepository.findAll();

        // then
        assertThat(allLikes).hasSize(3);
    }

    @Test
    @DisplayName("ID로 좋아요를 조회할 수 있다")
    void findById_ReturnsLike() {
        // given
        Long likeId = like1.getId();

        // when
        Like foundLike = likeRepository.findById(likeId).orElse(null);

        // then
        assertThat(foundLike).isNotNull();
        assertThat(foundLike.getProductId()).isEqualTo(1L);
        assertThat(foundLike.getMemberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 빈 값을 반환한다")
    void findById_WhenNotExists_ReturnsEmptyOptional() {
        // given
        Long nonExistentId = 999L;

        // when
        var result = likeRepository.findById(nonExistentId);

        // then
        assertThat(result).isEmpty();
    }
}
