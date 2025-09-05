package com.talktrip.talktrip.domain.like.repository;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")

        product2 = Product.builder()
                .productName("부산 여행")
                .description("바다가 아름다운 부산 여행")
                .thumbnailImageUrl("https://example.com/busan.jpg")
                .deleted(false)
                .build();

        deletedProduct = Product.builder()
                .productName("삭제된 상품")
                .description("삭제된 상품입니다")
                .thumbnailImageUrl("https://example.com/deleted.jpg")
                .deleted(true)
                .build();

        productRepository.saveAll(List.of(product1, product2, deletedProduct));

        // 테스트용 상품 옵션 생성
        ProductOption option1 = ProductOption.builder()
                .product(product1)
                .price(10000)
                .discountPrice(8000)
                .build();

        ProductOption option2 = ProductOption.builder()
                .product(product2)
                .price(15000)
                .discountPrice(12000)
                .build();

        productOptionRepository.saveAll(List.of(option1, option2));

        // 테스트용 리뷰 생성
        Review review1 = Review.builder()
                .product(product1)
                .reviewStar(4.5f)
                .build();

        Review review2 = Review.builder()
                .product(product2)
                .reviewStar(3.8f)
                .build();

        reviewRepository.saveAll(List.of(review1, review2));

        // 테스트용 좋아요 생성
        Like like1 = Like.builder()
                .productId(product1.getId())
                .memberId(1L)
                .build();

        Like like2 = Like.builder()
                .productId(product2.getId())
                .memberId(1L)
                .build();

        Like like3 = Like.builder()
                .productId(product1.getId())
                .memberId(2L)
                .build();

        likeRepository.saveAll(List.of(like1, like2, like3));
    }

    @Test
    @DisplayName("좋아요가 존재하는 경우 true를 반환한다")
    void existsByProductIdAndMemberId_WhenLikeExists_ReturnsTrue() {
        // given
        Long productId = product1.getId();
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
        Long productId = product1.getId();

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
        List<Long> productIds = List.of(product1.getId(), product2.getId(), 999L);

        // when
        Set<Long> result = likeRepository.findLikedProductIds(memberId, productIds);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).contains(1L, 2L);
        assertThat(result).doesNotContain(999L);
    }

    @Test
    @DisplayName("좋아요한 상품이 없는 경우 빈 Set을 반환한다")
    void findLikedProductIds_WhenNoLikes_ReturnsEmptySet() {
        // given
        Long memberId = 999L;
        List<Long> productIds = List.of(product1.getId(), product2.getId());

        // when
        Set<Long> result = likeRepository.findLikedProductIds(memberId, productIds);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("memberId가 null인 경우 빈 Set을 반환한다")
    void findLikedProductIds_WhenMemberIdIsNull_ReturnsEmptySet() {
        // given
        List<Long> productIds = List.of(product1.getId(), product2.getId());

        // when
        Set<Long> result = likeRepository.findLikedProductIds(null, productIds);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("productIds가 null인 경우 빈 Set을 반환한다")
    void findLikedProductIds_WhenProductIdsIsNull_ReturnsEmptySet() {
        // given
        Long memberId = 1L;

        // when
        Set<Long> result = likeRepository.findLikedProductIds(memberId, null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("productIds가 빈 리스트인 경우 빈 Set을 반환한다")
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

}
