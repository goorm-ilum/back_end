package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.product.dto.response.ProductDetailResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.*;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks ProductService productService;
    @Mock ProductRepository productRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock LikeRepository likeRepository;

    private Member seller() {
        return Member.builder()
                .Id(SELLER_ID)
                .name(SELLER_NAME)
                .accountEmail(SELLER_EMAIL)
                .phoneNum(PHONE_NUMBER)
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
    }

    private Member user() {
        return Member.builder()
                .Id(USER_ID)
                .name(USER_NAME)
                .accountEmail(USER_EMAIL)
                .phoneNum(PHONE_NUMBER)
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
    }

    private Member user2() {
        return Member.builder()
                .Id(USER_ID2)
                .name(USER2_NAME)
                .accountEmail(USER2_EMAIL)
                .phoneNum(PHONE_NUMBER)
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
    }

    private Country kr() {
        return Country.builder().name(COUNTRY_KOREA).continent(CONTINENT_ASIA).build();
    }

    private Product productWithFutureOption(Long id, String name, int discountPrice) {
        Product p = Product.builder()
                .id(id) // 빌더에 id 포함 가정
                .member(seller())
                .productName(name)
                .description(DESC)
                .thumbnailImageUrl(THUMBNAIL_URL)
                .country(kr())
                .deleted(false)
                .build();

        p.getProductOptions().add(ProductOption.builder()
                .id(id + 100) // 테스트용 고유 id
                .product(p)
                .optionName(OPTION_NAME)
                .startDate(LocalDate.now())
                .stock(STOCK_5)
                .price(PRICE_12000)
                .discountPrice(discountPrice)
                .build());
        return p;
    }

    private List<Review> reviews_4_and_2(Product p) {
        return List.of(
                Review.builder().product(p).member(user()).reviewStar(STAR_4_0).build(),
                Review.builder().product(p).member(user2()).reviewStar(STAR_2_0).build()
        );
    }

    // ===== searchProducts =====
    @Nested @DisplayName("searchProducts(keyword, countryName, memberId, pageable)")
    class SearchProducts {

        @Test
        @DisplayName("키워드 없음 + country=전체 → findVisibleProducts 위임, 빈 페이지 그대로 매핑")
        void noKeyword_allCountry_empty() {
            Pageable pageable = PAGE_0_SIZE_9;
            given(productRepository.findVisibleProducts(COUNTRY_ALL, pageable))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            Page<ProductSummaryResponse> res =
                    productService.searchProducts(null, COUNTRY_ALL, null, pageable);

            assertThat(res.getTotalElements()).isZero();
            assertThat(res.getContent()).isEmpty();

            then(productRepository).should().findVisibleProducts(COUNTRY_ALL, pageable);
            then(reviewRepository).shouldHaveNoInteractions();
            then(likeRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("키워드 공백 + 특정 국가 → findVisibleProducts 위임 + 평균/좋아요(배치) 매핑")
        void blankKeyword_byCountry_data() {
            Pageable pageable = PAGE_0_SIZE_9;
            Product p = productWithFutureOption(PRODUCT_ID, PRODUCT_NAME_1, DISC_9000);

            given(productRepository.findVisibleProducts(COUNTRY_KOREA, pageable))
                    .willReturn(new PageImpl<>(List.of(p), pageable, 1));
            given(reviewRepository.fetchAvgStarsByProductIds(List.of(PRODUCT_ID)))
                    .willReturn(Map.of(PRODUCT_ID, 3.0));
            given(likeRepository.findLikedProductIds(USER_ID, List.of(PRODUCT_ID)))
                    .willReturn(Set.of(PRODUCT_ID));

            Page<ProductSummaryResponse> res =
                    productService.searchProducts(BLANK, COUNTRY_KOREA, USER_ID, pageable);

            assertThat(res.getTotalElements()).isEqualTo(1);
            ProductSummaryResponse dto = res.getContent().getFirst();
            assertThat(dto.productId()).isEqualTo(PRODUCT_ID);
            assertThat(dto.averageReviewStar()).isEqualTo(AVG_3_0);
            assertThat(dto.isLiked()).isTrue();

            then(productRepository).should().findVisibleProducts(COUNTRY_KOREA, pageable);
            then(reviewRepository).should().fetchAvgStarsByProductIds(List.of(PRODUCT_ID));
            then(likeRepository).should().findLikedProductIds(USER_ID, List.of(PRODUCT_ID));
        }

        @Test
        @DisplayName("키워드 여러개 → searchByKeywords 위임 + 키워드 전달 검증 + 배치 매핑")
        void keywords_passthrough_and_batch_mapping() {
            Pageable pageable = PAGE_0_SIZE_9;

            Product a = productWithFutureOption(PRODUCT_ID, PRODUCT_NAME_1, DISC_9000);
            Product b = productWithFutureOption(OTHER_PRODUCT_ID, PRODUCT_NAME_2, DISC_9500);

            String multi = PRODUCT_NAME_1 + " " + HASHTAG_SEA;
            List<String> expectedKeywords = List.of(PRODUCT_NAME_1, HASHTAG_SEA);

            given(productRepository.searchByKeywords(expectedKeywords, COUNTRY_KOREA, pageable))
                    .willReturn(new PageImpl<>(List.of(a, b), pageable, 2));
            given(reviewRepository.fetchAvgStarsByProductIds(List.of(PRODUCT_ID, OTHER_PRODUCT_ID)))
                    .willReturn(Map.of(PRODUCT_ID, 3.0, OTHER_PRODUCT_ID, 5.0));

            Page<ProductSummaryResponse> res =
                    productService.searchProducts(multi, COUNTRY_KOREA, null, pageable);

            assertThat(res.getTotalElements()).isEqualTo(2);
            assertThat(res.getContent()).extracting(ProductSummaryResponse::averageReviewStar)
                    .containsExactly(AVG_3_0, AVG_5_0);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> cap = ArgumentCaptor.forClass(List.class);
            then(productRepository).should()
                    .searchByKeywords(cap.capture(), eq(COUNTRY_KOREA), eq(pageable));
            assertThat(cap.getValue()).containsExactlyElementsOf(expectedKeywords);

            then(likeRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("레포지토리 정렬/페이지 결과를 그대로 유지(서비스는 순서 변경 없음)")
        void keeps_repository_order() {
            Pageable pageable = PageRequest.of(PAGE_0, SIZE_2, DEFAULT_SORT_UPDATED_DESC);

            Product p1 = productWithFutureOption(PRODUCT_ID, PRODUCT_NAME_1, DISC_9000);
            Product p2 = productWithFutureOption(OTHER_PRODUCT_ID, PRODUCT_NAME_2, DISC_9500);

            // repo가 정렬 결과를 [p2, p1]로 줬다고 가정
            given(productRepository.findVisibleProducts(COUNTRY_ALL, pageable))
                    .willReturn(new PageImpl<>(List.of(p2, p1), pageable, 2));
            given(reviewRepository.fetchAvgStarsByProductIds(List.of(OTHER_PRODUCT_ID, PRODUCT_ID)))
                    .willReturn(Map.of(OTHER_PRODUCT_ID, 5.0, PRODUCT_ID, 3.0));

            Page<ProductSummaryResponse> res =
                    productService.searchProducts(null, COUNTRY_ALL, null, pageable);

            assertThat(res.getContent()).extracting(ProductSummaryResponse::productId)
                    .containsExactly(OTHER_PRODUCT_ID, PRODUCT_ID);
        }

        @Test
        @DisplayName("page overflow는 레포지토리 반환을 그대로 전달")
        void page_overflow_passthrough() {
            Pageable overflow = PageRequest.of(PAGE_2, SIZE_2, DEFAULT_SORT_UPDATED_DESC);
            given(productRepository.findVisibleProducts(COUNTRY_ALL, overflow))
                    .willReturn(new PageImpl<>(List.of(), overflow, 2));

            Page<ProductSummaryResponse> res =
                    productService.searchProducts(null, COUNTRY_ALL, null, overflow);

            assertThat(res.getContent()).isEmpty();
            assertThat(res.getTotalElements()).isEqualTo(2);
            assertThat(res.getNumber()).isEqualTo(PAGE_2);
            assertThat(res.getSize()).isEqualTo(SIZE_2);

            then(reviewRepository).shouldHaveNoInteractions();
            then(likeRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("memberId=null → 좋아요 배치 조회 미호출(게스트)")
        void guest_should_not_call_like_repo() {
            Pageable pageable = PAGE_0_SIZE_9;
            Product p = productWithFutureOption(PRODUCT_ID, PRODUCT_NAME_1, DISC_9000);

            given(productRepository.findVisibleProducts(COUNTRY_ALL, pageable))
                    .willReturn(new PageImpl<>(List.of(p), pageable, 1));
            given(reviewRepository.fetchAvgStarsByProductIds(List.of(PRODUCT_ID)))
                    .willReturn(Map.of(PRODUCT_ID, 3.0));

            Page<ProductSummaryResponse> res =
                    productService.searchProducts(null, COUNTRY_ALL, null, pageable);

            assertThat(res.getTotalElements()).isEqualTo(1);
            then(likeRepository).shouldHaveNoInteractions();
        }
    }

    @Nested @DisplayName("getProductDetail(productId, memberId, pageable)")
    class GetProductDetail {

        @Test
        @DisplayName("정상: 평균 별점은 전체 리뷰 기준, isLiked=false")
        void ok_guest() {
            Product p = productWithFutureOption(PRODUCT_ID, PRODUCT_NAME_1, DISC_9000);
            List<Review> all = reviews_4_and_2(p);

            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(p));
            Page<Review> page = new PageImpl<>(all, PAGE_0_SIZE_9, all.size());
            given(reviewRepository.findByProductId(eq(PRODUCT_ID), any(Pageable.class))).willReturn(page);
            given(reviewRepository.findByProductId(PRODUCT_ID)).willReturn(all);

            ProductDetailResponse res =
                    productService.getProductDetail(PRODUCT_ID, null, PAGE_0_SIZE_9);

            assertThat(res.productId()).isEqualTo(PRODUCT_ID);
            assertThat(res.averageReviewStar()).isEqualTo(AVG_3_0);
            assertThat(res.isLiked()).isFalse();
        }

        @Test
        @DisplayName("로그인: isLiked=true + 평균은 전체 리뷰 기준(페이지 일부만 조회돼도 전체 평균)")
        void authed_isLiked_true_and_avg_from_all_reviews() {
            Product p = productWithFutureOption(PRODUCT_ID, PRODUCT_NAME_1, DISC_9000);
            List<Review> all = reviews_4_and_2(p);
            Review only = all.getFirst();

            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(p));
            Page<Review> partial = new PageImpl<>(List.of(only), PAGE_0_SIZE_9, 1);
            given(reviewRepository.findByProductId(eq(PRODUCT_ID), any(Pageable.class))).willReturn(partial);
            given(reviewRepository.findByProductId(PRODUCT_ID)).willReturn(all);
            given(likeRepository.existsByProductIdAndMemberId(PRODUCT_ID, USER_ID)).willReturn(true);

            ProductDetailResponse res =
                    productService.getProductDetail(PRODUCT_ID, USER_ID, PAGE_0_SIZE_9);

            assertThat(res.productId()).isEqualTo(PRODUCT_ID);
            assertThat(res.isLiked()).isTrue();
            assertThat(res.averageReviewStar()).isEqualTo(AVG_3_0);
        }

        @Test
        @DisplayName("상품 없음 → PRODUCT_NOT_FOUND")
        void productMissing() {
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());
            assertThatThrownBy(() ->
                    productService.getProductDetail(PRODUCT_ID, USER_ID, PAGE_0_SIZE_9)
            ).isInstanceOf(ProductException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test
        @DisplayName("미래 재고 합 0 → PRODUCT_NOT_FOUND")
        void futureStockZero_throws() {
            Product p = productWithFutureOption(PRODUCT_ID, PRODUCT_NAME_1, DISC_9000);
            p.getProductOptions().clear(); // 미래 재고 0
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(p));

            assertThatThrownBy(() ->
                    productService.getProductDetail(PRODUCT_ID, null, PAGE_0_SIZE_9)
            ).isInstanceOf(ProductException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}
