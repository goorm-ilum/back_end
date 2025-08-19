package com.talktrip.talktrip.domain.like.service;

import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @InjectMocks LikeService likeService;
    @Mock LikeRepository likeRepository;
    @Mock ProductRepository productRepository;
    @Mock MemberRepository memberRepository;

    private CustomMemberDetails memberDetails(long id) {
        CustomMemberDetails md = mock(CustomMemberDetails.class);
        when(md.getId()).thenReturn(id);
        return md;
    }

    private Member member(long id, String email, MemberRole role) {
        return Member.builder()
                .Id(id)
                .accountEmail(email)
                .memberRole(role)
                .memberState(MemberState.A)
                .build();
    }

    private Product product(long id, Member member) {
        return Product.builder()
                .id(id)
                .member(member)
                .productName("P1")
                .description("test description")
                .deleted(false)
                .build();
    }

    @Nested
    @DisplayName("toggleLike")
    class ToggleLike {

        @Test
        @DisplayName("이미 좋아요가 존재하면 삭제")
        void whenExists_delete() {
            // given
            long productId = 1L, userId = 1L;
            CustomMemberDetails md = memberDetails(userId);
            given(likeRepository.existsByProductIdAndMemberId(productId, userId)).willReturn(true);

            // when
            likeService.toggleLike(productId, md);

            // then
            then(likeRepository).should().deleteByProductIdAndMemberId(productId, userId);
            then(productRepository).shouldHaveNoInteractions();
            then(memberRepository).shouldHaveNoInteractions();
            then(likeRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("좋아요가 없으면 상품/회원 조회 후 저장")
        void whenNotExists_save() {
            long productId = 1L, userId = 1L, sellerId = 2L;
            CustomMemberDetails md = memberDetails(userId);
            Member member = member(userId, "user@gmail.com", MemberRole.U);
            Member seller = member(sellerId, "seller@gmail.com", MemberRole.A);
            Product product = product(productId, seller);

            given(likeRepository.existsByProductIdAndMemberId(productId, userId)).willReturn(false);
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(memberRepository.findById(userId)).willReturn(Optional.of(member));

            likeService.toggleLike(productId, md);

            then(likeRepository).should().save(argThat(l ->
                    l.getProduct() == product && l.getMember() == member));
        }

        @Test
        @DisplayName("좋아요 없음 + 상품 미존재 -> ProductException")
        void whenProductMissing_throw() {
            long productId = 1L, userId = 1L;
            CustomMemberDetails md = memberDetails(userId);

            given(likeRepository.existsByProductIdAndMemberId(productId, userId)).willReturn(false);
            given(productRepository.findById(productId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> likeService.toggleLike(productId, md))
                    .isInstanceOf(ProductException.class);
        }

        @Test
        @DisplayName("좋아요 없음 + 회원 미존재 -> MemberException")
        void whenMemberMissing_throw() {
            long productId = 1L, userId = 1L, sellerId = 2L;
            CustomMemberDetails md = memberDetails(userId);
            Member seller = member(sellerId, "seller@gmail.com", MemberRole.A);
            given(likeRepository.existsByProductIdAndMemberId(productId, userId)).willReturn(false);
            given(productRepository.findById(productId)).willReturn(Optional.of(product(productId, seller)));
            given(memberRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> likeService.toggleLike(productId, md))
                    .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("예외: 인증 정보 없음 -> MemberException")
        void NoPrincipal() {
            assertThatThrownBy(() -> likeService.toggleLike(1L, null))
                    .isInstanceOf(MemberException.class);
        }
    }

    @Nested
    @DisplayName("getLikedProducts")
    class GetLikedProducts {

        @Test
        @DisplayName("리뷰 평균 계산 + liked=true 매핑")
        void reviewAverage() {
            long productId = 1L, userId = 1L, sellerId = 2L;
            CustomMemberDetails md = memberDetails(userId);
            Member member = member(userId, "user@gmail.com", MemberRole.U);
            Member seller = member(sellerId, "seller@gmail.com", MemberRole.A);
            Product p = product(productId, seller);
            Review r1 = Review.builder().reviewStar(5f).product(p).build();
            Review r2 = Review.builder().reviewStar(3f).product(p).build();
            p.getReviews().addAll(List.of(r1, r2));

            Like like = Like.builder().product(p).member(member).build();
            Page<Like> page = new PageImpl<>(List.of(like), PageRequest.of(0, 10), 1);

            given(likeRepository.findByMemberId(eq(userId), any(Pageable.class)))
                    .willReturn(page);

            Page<ProductSummaryResponse> res = likeService.getLikedProducts(md, PageRequest.of(0, 10));

            assertThat(res.getTotalElements()).isEqualTo(1);
            ProductSummaryResponse first = res.getContent().getFirst();
            assertThat(first.averageReviewStar()).isEqualTo(4.0f);
            assertThat(first.isLiked()).isTrue();
        }

        @Test
        @DisplayName("리뷰가 없으면 평균 0.0")
        void avgZeroWhenNoReviews() {
            long productId = 1L, userId = 1L, sellerId = 2L;
            CustomMemberDetails md = memberDetails(userId);
            Member member = member(userId, "user@gmail.com", MemberRole.U);
            Member seller = member(sellerId, "seller@gmail.com", MemberRole.A);

            Product p = product(productId, seller);
            Like like = Like.builder().product(p).member(member).build();
            Page<Like> page = new PageImpl<>(List.of(like), PageRequest.of(0, 10), 1);

            given(likeRepository.findByMemberId(eq(userId), any(Pageable.class)))
                    .willReturn(page);

            Page<ProductSummaryResponse> res = likeService.getLikedProducts(md, PageRequest.of(0, 10));
            assertThat(res.getContent().getFirst().averageReviewStar()).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("좋아요가 없으면 빈 페이지")
        void empty() {
            long userId = 1L;
            CustomMemberDetails md = memberDetails(userId);

            Page<Like> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(likeRepository.findByMemberId(eq(userId), any(Pageable.class)))
                    .willReturn(page);

            Page<ProductSummaryResponse> res = likeService.getLikedProducts(md, PageRequest.of(0, 10));
            assertThat(res.getContent()).isEmpty();
            assertThat(res.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("예외: 인증 없음 -> MemberException")
        void noPrincipal() {
            assertThatThrownBy(() -> likeService.getLikedProducts(null, PageRequest.of(0, 10)))
                    .isInstanceOf(MemberException.class);
        }
    }
}
