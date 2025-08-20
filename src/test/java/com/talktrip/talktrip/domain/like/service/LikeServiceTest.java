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
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @InjectMocks LikeService likeService;
    @Mock LikeRepository likeRepository;
    @Mock ProductRepository productRepository;
    @Mock MemberRepository memberRepository;

    private Member user() {
        return Member.builder()
                .Id(USER_ID).accountEmail(USER_EMAIL)
                .memberRole(MemberRole.U).memberState(MemberState.A)
                .build();
    }

    private Member seller() {
        return Member.builder()
                .Id(SELLER_ID).accountEmail(SELLER_EMAIL)
                .memberRole(MemberRole.A).memberState(MemberState.A)
                .build();
    }

    private Product product() {
        return Product.builder()
                .id(PRODUCT_ID).member(seller())
                .productName(PRODUCT_NAME_1).description(DESC)
                .deleted(false)
                .build();
    }

    @Nested @DisplayName("toggleLike(productId, memberId)")
    class ToggleLike {

        @Test @DisplayName("이미 존재 → 삭제")
        void whenExists_delete() {
            given(likeRepository.existsByProductIdAndMemberId(PRODUCT_ID, USER_ID)).willReturn(true);

            likeService.toggleLike(PRODUCT_ID, USER_ID);

            then(likeRepository).should().deleteByProductIdAndMemberId(PRODUCT_ID, USER_ID);
            then(productRepository).shouldHaveNoInteractions();
            then(memberRepository).shouldHaveNoInteractions();
            then(likeRepository).should(never()).save(any());
        }

        @Test @DisplayName("없으면 상품/회원 조회 후 저장")
        void whenNotExists_save() {
            Product p = product();
            Member u = user();
            given(likeRepository.existsByProductIdAndMemberId(PRODUCT_ID, USER_ID)).willReturn(false);
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(p));
            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(u));

            likeService.toggleLike(PRODUCT_ID, USER_ID);

            then(likeRepository).should().save(argThat(l -> l.getProduct() == p && l.getMember() == u));
        }

        @Test @DisplayName("PRODUCT_NOT_FOUND")
        void productMissing() {
            given(likeRepository.existsByProductIdAndMemberId(PRODUCT_ID, USER_ID)).willReturn(false);
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> likeService.toggleLike(PRODUCT_ID, USER_ID))
                    .isInstanceOf(ProductException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test @DisplayName("USER_NOT_FOUND")
        void userMissing() {
            given(likeRepository.existsByProductIdAndMemberId(PRODUCT_ID, USER_ID)).willReturn(false);
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product()));
            given(memberRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> likeService.toggleLike(PRODUCT_ID, USER_ID))
                    .isInstanceOf(MemberException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested @DisplayName("getLikedProducts(memberId, pageable)")
    class GetLikedProducts {

        @Test @DisplayName("USER_NOT_FOUND")
        void userMissing() {
            given(memberRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> likeService.getLikedProducts(USER_ID, PageRequest.of(PAGE_0, SIZE_9)))
                    .isInstanceOf(MemberException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test @DisplayName("빈 페이지 반환")
        void empty() {
            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            Pageable expected = PageRequest.of(PAGE_0, SIZE_9, SORT_BY_UPDATED_DESC);
            Page<Like> page = new PageImpl<>(List.of(), expected, 0);
            given(likeRepository.findByMemberId(eq(USER_ID), any(Pageable.class))).willReturn(page);

            Page<ProductSummaryResponse> res = likeService.getLikedProducts(USER_ID, expected);

            assertThat(res.getContent()).isEmpty();
            assertThat(res.getTotalElements()).isZero();
            assertThat(res.getNumber()).isEqualTo(PAGE_0);
            assertThat(res.getSize()).isEqualTo(SIZE_9);

            ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
            then(likeRepository).should().findByMemberId(eq(USER_ID), pageableCap.capture());
            Pageable sent = pageableCap.getValue();
            assertThat(sent.getSort()).isEqualTo(SORT_BY_UPDATED_DESC);
            assertThat(sent.getPageNumber()).isEqualTo(PAGE_0);
            assertThat(sent.getPageSize()).isEqualTo(SIZE_9);
        }

        @Test @DisplayName("정상 조회: 여러 좋아요 → 평균/liked 매핑")
        void success_multipleLikes() {
            Member u = user();

            Product p1 = product();
            p1.getReviews().addAll(List.of(
                    Review.builder().product(p1).reviewStar(STAR_4_0).build(),
                    Review.builder().product(p1).reviewStar(STAR_2_0).build()
            ));
            Like like1 = Like.builder().product(p1).member(u).build();

            Product p2 = Product.builder()
                    .id(OTHER_PRODUCT_ID).member(seller())
                    .productName(PRODUCT_NAME_2).description(DESC)
                    .deleted(false).build();
            p2.getReviews().add(Review.builder().product(p2).reviewStar(STAR_5_0).build());
            Like like2 = Like.builder().product(p2).member(u).build();

            Page<Like> page = new PageImpl<>(List.of(like1, like2), PageRequest.of(PAGE_0, SIZE_9), 2);

            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(u));
            given(likeRepository.findByMemberId(eq(USER_ID), any(Pageable.class))).willReturn(page);

            Page<ProductSummaryResponse> res = likeService.getLikedProducts(USER_ID, PageRequest.of(PAGE_0, SIZE_9));

            assertThat(res.getTotalElements()).isEqualTo(2);
            assertThat(res.getContent()).hasSize(2);

            ProductSummaryResponse r1 = res.getContent().getFirst();
            assertThat(r1.productId()).isEqualTo(PRODUCT_ID);
            assertThat(r1.productName()).isEqualTo(PRODUCT_NAME_1);
            assertThat(r1.averageReviewStar()).isEqualTo(AVG_3_0);
            assertThat(r1.isLiked()).isTrue();

            ProductSummaryResponse r2 = res.getContent().get(1);
            assertThat(r2.productId()).isEqualTo(OTHER_PRODUCT_ID);
            assertThat(r2.productName()).isEqualTo(PRODUCT_NAME_2);
            assertThat(r2.averageReviewStar()).isEqualTo(AVG_5_0);
            assertThat(r2.isLiked()).isTrue();
        }

        @Test @DisplayName("초과 페이지 → 빈 페이지 반환(문서화)")
        void overflow_passthrough() {
            given(memberRepository.findById(USER_ID)).willReturn(Optional.of(user()));
            Pageable overflow = PageRequest.of(PAGE_3, SIZE_10, SORT_BY_UPDATED_DESC);
            Page<Like> page = new PageImpl<>(List.of(), overflow, 2);
            given(likeRepository.findByMemberId(eq(USER_ID), any(Pageable.class))).willReturn(page);

            Page<ProductSummaryResponse> res = likeService.getLikedProducts(USER_ID, overflow);

            assertThat(res.getContent()).isEmpty();
            assertThat(res.getTotalElements()).isEqualTo(2);
            assertThat(res.getNumber()).isEqualTo(PAGE_3);
            assertThat(res.getSize()).isEqualTo(SIZE_10);
        }
    }
}
