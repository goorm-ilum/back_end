package com.talktrip.talktrip.domain.like.service;

import com.talktrip.talktrip.domain.like.dto.ProductWithAvgStar;
import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private LikeService likeService;

    private Member member;
    private Product product;
    private Like like;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .Id(1L)
                .accountEmail("test@test.com")
                .phoneNum("010-1234-5678")
                .name("테스트유저")
                .nickname("테스트유저")
                .build();

        product = Product.builder()
                .id(1L)
                .productName("제주도 여행")
                .description("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .member(member)
                .build();

        like = Like.builder()
                .id(1L)
                .productId(product.getId())
                .memberId(member.getId())
                .build();
    }

    @Test
    @DisplayName("좋아요를 토글할 수 있다 - 좋아요 추가")
    void toggleLike_AddLike() {
        // given
        Long memberId = member.getId();
        Long productId = product.getId();

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.existsById(productId)).thenReturn(true);
        when(likeRepository.existsByProductIdAndMemberId(productId, memberId)).thenReturn(false);
        when(likeRepository.save(any(Like.class))).thenReturn(like);

        // when
        likeService.toggleLike(productId, memberId);

        // then
        verify(likeRepository).save(any(Like.class));
        verify(likeRepository, never()).deleteByProductIdAndMemberId(any(), any());
    }

    @Test
    @DisplayName("좋아요를 토글할 수 있다 - 좋아요 취소")
    void toggleLike_RemoveLike() {
        // given
        Long memberId = member.getId();
        Long productId = product.getId();

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.existsById(productId)).thenReturn(true);
        when(likeRepository.existsByProductIdAndMemberId(productId, memberId)).thenReturn(true);

        // when
        likeService.toggleLike(productId, memberId);

        // then
        verify(likeRepository).deleteByProductIdAndMemberId(productId, memberId);
        verify(likeRepository, never()).save(any(Like.class));
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 예외가 발생한다")
    void toggleLike_MemberNotFound() {
        // given
        Long nonExistentMemberId = 999L;
        Long productId = product.getId();

        when(memberRepository.existsById(nonExistentMemberId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> likeService.toggleLike(productId, nonExistentMemberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 상품이면 예외가 발생한다")
    void toggleLike_ProductNotFound() {
        // given
        Long memberId = member.getId();
        Long nonExistentProductId = 999L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.existsById(nonExistentProductId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> likeService.toggleLike(nonExistentProductId, memberId))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("null 회원 ID면 예외가 발생한다")
    void toggleLike_NullMemberId() {
        // given
        Long productId = product.getId();

        // when & then
        assertThatThrownBy(() -> likeService.toggleLike(productId, null))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("null 상품 ID면 예외가 발생한다")
    void toggleLike_NullProductId() {
        // given
        Long memberId = member.getId();

        // when
        when(memberRepository.existsById(memberId)).thenReturn(true);

        // then
        assertThatThrownBy(() -> likeService.toggleLike(null, memberId))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("좋아요 상품 목록을 조회할 수 있다")
    void getLikedProducts() {
        // given
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 9);
        
        // ProductWithAvgStar 객체 생성
        ProductWithAvgStar productWithAvgStar = new ProductWithAvgStar(product, 4.5);
        Page<ProductWithAvgStar> productWithAvgStarPage = new PageImpl<>(List.of(productWithAvgStar), pageable, 1);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(likeRepository.findLikedProductsWithAvgStar(memberId, pageable)).thenReturn(productWithAvgStarPage);

        // when
        Page<ProductSummaryResponse> result = likeService.getLikedProducts(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productName()).isEqualTo("제주도 여행");
        assertThat(result.getContent().get(0).isLiked()).isTrue();
        assertThat(result.getContent().get(0).averageReviewStar()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 좋아요 목록 조회 시 예외가 발생한다")
    void getLikedProducts_MemberNotFound() {
        // given
        Long nonExistentMemberId = 999L;
        Pageable pageable = PageRequest.of(0, 9);

        when(memberRepository.existsById(nonExistentMemberId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> likeService.getLikedProducts(nonExistentMemberId, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("null 회원 ID로 좋아요 목록 조회 시 예외가 발생한다")
    void getLikedProducts_NullMemberId() {
        // given
        Pageable pageable = PageRequest.of(0, 9);

        // when & then
        assertThatThrownBy(() -> likeService.getLikedProducts(null, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("좋아요 목록 조회 시 페이징이 올바르게 적용된다")
    void getLikedProducts_WithPaging() {
        // given
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(1, 5); // 두 번째 페이지, 크기 5
        
        ProductWithAvgStar productWithAvgStar = new ProductWithAvgStar(product, 4.5);
        Page<ProductWithAvgStar> productWithAvgStarPage = new PageImpl<>(List.of(productWithAvgStar), pageable, 10);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(likeRepository.findLikedProductsWithAvgStar(memberId, pageable)).thenReturn(productWithAvgStarPage);

        // when
        Page<ProductSummaryResponse> result = likeService.getLikedProducts(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("좋아요 목록이 비어있을 때 빈 페이지를 반환한다")
    void getLikedProducts_EmptyList() {
        // given
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);
        
        Page<ProductWithAvgStar> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(likeRepository.findLikedProductsWithAvgStar(memberId, pageable)).thenReturn(emptyPage);

        // when
        Page<ProductSummaryResponse> result = likeService.getLikedProducts(memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }
}
