package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.ProductWithAvgStarAndLike;
import com.talktrip.talktrip.domain.product.dto.response.ProductDetailResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.repository.CountryRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ProductService productService;

    private Member member;
    private Product product;
    private Country country;
    private Review review;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .Id(1L)
                .accountEmail("test@test.com")
                .phoneNum("010-1234-5678")
                .name("테스트유저")
                .nickname("테스트유저")
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();

        country = Country.builder()
                .id(1L)
                .name("대한민국")
                .build();

        product = Product.builder()
                .id(1L)
                .productName("제주도 여행")
                .description("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .member(member)
                .country(country)
                .build();

        review = Review.builder()
                .id(1L)
                .member(member)
                .product(product)
                .comment("좋은 여행이었습니다")
                .reviewStar(4.5)
                .build();

        ReflectionTestUtils.setField(productService, "fastApiBaseUrl", "http://localhost:8000");
    }

    @Test
    @DisplayName("상품 검색을 성공적으로 수행한다")
    void searchProducts_Success() {
        // given
        String keyword = "제주도";
        String countryName = "전체";
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);
        
        // Mock 객체 생성
        ProductWithAvgStarAndLike productWithAvgStarAndLike = mock(ProductWithAvgStarAndLike.class);
        when(productWithAvgStarAndLike.getProduct()).thenReturn(product);
        when(productWithAvgStarAndLike.getAvgStar()).thenReturn(4.5);
        when(productWithAvgStarAndLike.getIsLiked()).thenReturn(true);
        
        Page<ProductWithAvgStarAndLike> searchResults = new PageImpl<>(List.of(productWithAvgStarAndLike), pageable, 1);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.searchProductsWithAvgStarAndLike(eq(keyword), eq("전체"), eq(memberId), eq(pageable)))
                .thenReturn(searchResults);

        // when
        Page<ProductSummaryResponse> result = productService.searchProducts(keyword, countryName, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productName()).isEqualTo("제주도 여행");
        assertThat(result.getContent().get(0).isLiked()).isTrue();
        assertThat(result.getContent().get(0).averageReviewStar()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("상품 검색 시 키워드가 없으면 null로 처리한다")
    void searchProducts_EmptyKeyword() {
        // given
        String countryName = "전체";
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);
        
        // Mock 객체 생성
        ProductWithAvgStarAndLike productWithAvgStarAndLike = mock(ProductWithAvgStarAndLike.class);
        when(productWithAvgStarAndLike.getProduct()).thenReturn(product);
        when(productWithAvgStarAndLike.getAvgStar()).thenReturn(4.5);
        when(productWithAvgStarAndLike.getIsLiked()).thenReturn(true);
        
        Page<ProductWithAvgStarAndLike> searchResults = new PageImpl<>(List.of(productWithAvgStarAndLike), pageable, 1);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.searchProductsWithAvgStarAndLike(eq(""), eq("전체"), eq(memberId), eq(pageable)))
                .thenReturn(searchResults);

        // when
        Page<ProductSummaryResponse> result = productService.searchProducts("", countryName, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productName()).isEqualTo("제주도 여행");
    }

    @Test
    @DisplayName("상품 검색 결과가 없으면 빈 페이지를 반환한다")
    void searchProducts_EmptyResult() {
        // given
        String keyword = "존재하지않는상품";
        String countryName = "전체";
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductWithAvgStarAndLike> emptyResults = new PageImpl<>(List.of(), pageable, 0);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.searchProductsWithAvgStarAndLike(eq(keyword), eq("전체"), eq(memberId), eq(pageable)))
                .thenReturn(emptyResults);

        // when
        Page<ProductSummaryResponse> result = productService.searchProducts(keyword, countryName, memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 상품 검색 시 예외가 발생한다")
    void searchProducts_MemberNotFound() {
        // given
        Long nonExistentMemberId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(nonExistentMemberId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> productService.searchProducts("제주도", "전체", nonExistentMemberId, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("null 회원 ID로 상품 검색 시 예외가 발생한다")
    void searchProducts_NullMemberId() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.searchProducts("제주도", "전체", null, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 상세 정보를 성공적으로 조회한다")
    void getProductDetail_Success() {
        // given
        Long productId = product.getId();
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 3);
        Page<Review> reviewPage = new PageImpl<>(List.of(review), pageable, 1);

        ProductWithAvgStarAndLike productWithDetails = ProductWithAvgStarAndLike.builder()
                .product(product)
                .avgStar(4.5)
                .isLiked(true)
                .build();

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productRepository.findByIdWithDetailsAndAvgStarAndLike(eq(productId), eq(memberId)))
                .thenReturn(Optional.of(productWithDetails));
        when(reviewRepository.findByProductIdWithPaging(productId, pageable)).thenReturn(reviewPage);

        // when
        ProductDetailResponse result = productService.getProductDetail(productId, memberId, pageable);

        // then
        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.productName()).isEqualTo("제주도 여행");
        assertThat(result.isLiked()).isTrue();
        assertThat(result.averageReviewStar()).isEqualTo(4.5);
        assertThat(result.reviews()).hasSize(1);
    }

    @Test
    @DisplayName("상품 상세 조회 시 리뷰 페이징이 올바르게 적용된다")
    void getProductDetail_WithReviewPaging() {
        // given
        Long productId = product.getId();
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(1, 2); // 두 번째 페이지, 크기 2
        
        // 두 번째 페이지의 리뷰들 생성
        Review secondPageReview1 = Review.builder()
                .id(4L)
                .product(product)
                .member(member)
                .reviewStar(4.0)
                .comment("두 번째 페이지 리뷰 1")
                .build();
        
        Review secondPageReview2 = Review.builder()
                .id(5L)
                .product(product)
                .member(member)
                .reviewStar(5.0)
                .comment("두 번째 페이지 리뷰 2")
                .build();
        
        Page<Review> reviewPage = new PageImpl<>(List.of(secondPageReview1, secondPageReview2), pageable, 5); // 총 5개 리뷰

        ProductWithAvgStarAndLike productWithDetails = ProductWithAvgStarAndLike.builder()
                .product(product)
                .avgStar(4.5)
                .isLiked(true)
                .build();

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productRepository.findByIdWithDetailsAndAvgStarAndLike(eq(productId), eq(memberId)))
                .thenReturn(Optional.of(productWithDetails));
        when(reviewRepository.findByProductIdWithPaging(productId, pageable)).thenReturn(reviewPage);

        // when
        ProductDetailResponse result = productService.getProductDetail(productId, memberId, pageable);

        // then
        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.productName()).isEqualTo("제주도 여행");
        assertThat(result.isLiked()).isTrue();
        assertThat(result.averageReviewStar()).isEqualTo(4.5);
        assertThat(result.reviews()).hasSize(2);
        
        // 두 번째 페이지의 리뷰들이 올바르게 반환되는지 확인
        assertThat(result.reviews().get(0).reviewId()).isEqualTo(4L);
        assertThat(result.reviews().get(0).comment()).isEqualTo("두 번째 페이지 리뷰 1");
        assertThat(result.reviews().get(0).reviewStar()).isEqualTo(4.0);
        
        assertThat(result.reviews().get(1).reviewId()).isEqualTo(5L);
        assertThat(result.reviews().get(1).comment()).isEqualTo("두 번째 페이지 리뷰 2");
        assertThat(result.reviews().get(1).reviewStar()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("상품 상세 조회 시 리뷰가 없는 경우 빈 리스트를 반환한다")
    void getProductDetail_NoReviews() {
        // given
        Long productId = product.getId();
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 3);
        Page<Review> emptyReviewPage = new PageImpl<>(List.of(), pageable, 0);

        ProductWithAvgStarAndLike productWithDetails = ProductWithAvgStarAndLike.builder()
                .product(product)
                .avgStar(0.0)
                .isLiked(false)
                .build();

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productRepository.findByIdWithDetailsAndAvgStarAndLike(eq(productId), eq(memberId)))
                .thenReturn(Optional.of(productWithDetails));
        when(reviewRepository.findByProductIdWithPaging(productId, pageable)).thenReturn(emptyReviewPage);

        // when
        ProductDetailResponse result = productService.getProductDetail(productId, memberId, pageable);

        // then
        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.productName()).isEqualTo("제주도 여행");
        assertThat(result.isLiked()).isFalse();
        assertThat(result.averageReviewStar()).isEqualTo(0.0);
        assertThat(result.reviews()).isEmpty();
    }

    @Test
    @DisplayName("상품 상세 조회 시 리뷰 별점 내림차순 정렬이 올바르게 적용된다")
    void getProductDetail_WithReviewSortingDesc() {
        // given
        Long productId = product.getId();
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "reviewStar"));
        
        // 별점 내림차순으로 정렬된 리뷰들 생성
        Review highStarReview = Review.builder()
                .id(1L)
                .product(product)
                .member(member)
                .reviewStar(5.0)
                .comment("최고의 여행이었습니다!")
                .build();
        
        Review mediumStarReview = Review.builder()
                .id(2L)
                .product(product)
                .member(member)
                .reviewStar(4.5)
                .comment("좋은 여행이었습니다")
                .build();
        
        Review lowStarReview = Review.builder()
                .id(3L)
                .product(product)
                .member(member)
                .reviewStar(3.0)
                .comment("괜찮은 여행이었습니다")
                .build();
        
        Page<Review> reviewPage = new PageImpl<>(List.of(highStarReview, mediumStarReview, lowStarReview), pageable, 3);

        ProductWithAvgStarAndLike productWithDetails = ProductWithAvgStarAndLike.builder()
                .product(product)
                .avgStar(4.2)
                .isLiked(true)
                .build();

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productRepository.findByIdWithDetailsAndAvgStarAndLike(eq(productId), eq(memberId)))
                .thenReturn(Optional.of(productWithDetails));
        when(reviewRepository.findByProductIdWithPaging(productId, pageable)).thenReturn(reviewPage);

        // when
        ProductDetailResponse result = productService.getProductDetail(productId, memberId, pageable);

        // then
        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.productName()).isEqualTo("제주도 여행");
        assertThat(result.isLiked()).isTrue();
        assertThat(result.averageReviewStar()).isEqualTo(4.2);
        assertThat(result.reviews()).hasSize(3);
        
        // 별점 내림차순 정렬 검증
        assertThat(result.reviews().get(0).reviewId()).isEqualTo(1L);
        assertThat(result.reviews().get(0).reviewStar()).isEqualTo(5.0);
        assertThat(result.reviews().get(0).comment()).isEqualTo("최고의 여행이었습니다!");
        
        assertThat(result.reviews().get(1).reviewId()).isEqualTo(2L);
        assertThat(result.reviews().get(1).reviewStar()).isEqualTo(4.5);
        assertThat(result.reviews().get(1).comment()).isEqualTo("좋은 여행이었습니다");
        
        assertThat(result.reviews().get(2).reviewId()).isEqualTo(3L);
        assertThat(result.reviews().get(2).reviewStar()).isEqualTo(3.0);
        assertThat(result.reviews().get(2).comment()).isEqualTo("괜찮은 여행이었습니다");
    }

    @Test
    @DisplayName("상품 상세 조회 시 리뷰 별점 오름차순 정렬이 올바르게 적용된다")
    void getProductDetail_WithReviewSortingAsc() {
        // given
        Long productId = product.getId();
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "reviewStar"));
        
        // 별점 오름차순으로 정렬된 리뷰들 생성
        Review lowStarReview = Review.builder()
                .id(3L)
                .product(product)
                .member(member)
                .reviewStar(3.0)
                .comment("괜찮은 여행이었습니다")
                .build();
        
        Review mediumStarReview = Review.builder()
                .id(2L)
                .product(product)
                .member(member)
                .reviewStar(4.5)
                .comment("좋은 여행이었습니다")
                .build();
        
        Review highStarReview = Review.builder()
                .id(1L)
                .product(product)
                .member(member)
                .reviewStar(5.0)
                .comment("최고의 여행이었습니다!")
                .build();
        
        Page<Review> reviewPage = new PageImpl<>(List.of(lowStarReview, mediumStarReview, highStarReview), pageable, 3);

        ProductWithAvgStarAndLike productWithDetails = ProductWithAvgStarAndLike.builder()
                .product(product)
                .avgStar(4.2)
                .isLiked(true)
                .build();

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productRepository.findByIdWithDetailsAndAvgStarAndLike(eq(productId), eq(memberId)))
                .thenReturn(Optional.of(productWithDetails));
        when(reviewRepository.findByProductIdWithPaging(productId, pageable)).thenReturn(reviewPage);

        // when
        ProductDetailResponse result = productService.getProductDetail(productId, memberId, pageable);

        // then
        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.productName()).isEqualTo("제주도 여행");
        assertThat(result.isLiked()).isTrue();
        assertThat(result.averageReviewStar()).isEqualTo(4.2);
        assertThat(result.reviews()).hasSize(3);
        
        // 별점 오름차순 정렬 검증
        assertThat(result.reviews().get(0).reviewId()).isEqualTo(3L);
        assertThat(result.reviews().get(0).reviewStar()).isEqualTo(3.0);
        assertThat(result.reviews().get(0).comment()).isEqualTo("괜찮은 여행이었습니다");
        
        assertThat(result.reviews().get(1).reviewId()).isEqualTo(2L);
        assertThat(result.reviews().get(1).reviewStar()).isEqualTo(4.5);
        assertThat(result.reviews().get(1).comment()).isEqualTo("좋은 여행이었습니다");
        
        assertThat(result.reviews().get(2).reviewId()).isEqualTo(1L);
        assertThat(result.reviews().get(2).reviewStar()).isEqualTo(5.0);
        assertThat(result.reviews().get(2).comment()).isEqualTo("최고의 여행이었습니다!");
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 상세 조회 시 예외가 발생한다")
    void getProductDetail_ProductNotFound() {
        // given
        Long nonExistentProductId = 999L;
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 3);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.existsById(nonExistentProductId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(nonExistentProductId, memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 존재 확인은 true지만 상세 조회 Optional.empty()면 PRODUCT_NOT_FOUND")
    void getProductDetail_ExistsTrueButFindEmpty_ThrowsException() {
        Long productId = product.getId();
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 3);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productRepository.findByIdWithDetailsAndAvgStarAndLike(eq(productId), eq(memberId)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductDetail(productId, memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("null 상품 ID로 상세 조회 시 예외가 발생한다")
    void getProductDetail_NullProductId() {
        // given
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 3);

        when(memberRepository.existsById(memberId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(null, memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 상품 상세 조회 시 예외가 발생한다")
    void getProductDetail_MemberNotFound() {
        // given
        Long productId = product.getId();
        Long nonExistentMemberId = 999L;
        Pageable pageable = PageRequest.of(0, 3);

        when(memberRepository.existsById(nonExistentMemberId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(productId, nonExistentMemberId, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 국가로 상품 검색 시 예외가 발생한다")
    void searchProducts_NonExistentCountry_ThrowsException() {
        // given
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(countryRepository.existsByName("존재하지않는국가")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> productService.searchProducts("서울", "존재하지않는국가", memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUNTRY_NOT_FOUND);
    }

    @Test
    @DisplayName("전체 국가로 상품 검색 시 국가 검증을 건너뛴다")
    void searchProducts_AllCountries_SkipsValidation() {
        // given
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductWithAvgStarAndLike> productPage = new PageImpl<>(List.of(), pageable, 0);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.searchProductsWithAvgStarAndLike(any(), any(), eq(memberId), eq(pageable)))
                .thenReturn(productPage);

        // when
        Page<ProductSummaryResponse> result = productService.searchProducts("서울", "전체", memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("null 국가명으로 상품 검색 시 국가 검증을 건너뛴다")
    void searchProducts_NullCountry_SkipsValidation() {
        // given
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductWithAvgStarAndLike> productPage = new PageImpl<>(List.of(), pageable, 0);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.searchProductsWithAvgStarAndLike(any(), any(), eq(memberId), eq(pageable)))
                .thenReturn(productPage);

        // when
        Page<ProductSummaryResponse> result = productService.searchProducts("서울", null, memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("빈 문자열 국가명으로 상품 검색 시 국가 검증을 건너뛴다")
    void searchProducts_EmptyCountry_SkipsValidation() {
        // given
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductWithAvgStarAndLike> productPage = new PageImpl<>(List.of(), pageable, 0);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(productRepository.searchProductsWithAvgStarAndLike(any(), any(), eq(memberId), eq(pageable)))
                .thenReturn(productPage);

        // when
        Page<ProductSummaryResponse> result = productService.searchProducts("서울", "", memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("AI 상품 검색을 성공적으로 수행한다")
    void aiSearchProducts_Success() {
        // given
        String query = "제주도 여행";
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);
        List<Long> productIds = List.of(1L);
        
        // Mock 객체 생성
        ProductWithAvgStarAndLike productWithAvgStarAndLike = mock(ProductWithAvgStarAndLike.class);
        when(productWithAvgStarAndLike.getProduct()).thenReturn(product);
        when(productWithAvgStarAndLike.getAvgStar()).thenReturn(4.5);
        when(productWithAvgStarAndLike.getIsLiked()).thenReturn(true);
        
        List<ProductWithAvgStarAndLike> productsWithDetails = List.of(productWithAvgStarAndLike);
        Page<ProductWithAvgStarAndLike> productPage = new PageImpl<>(productsWithDetails, pageable, 1);

        Map<String, Object> aiResponse = Map.of("product_ids", productIds);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(restTemplate.postForObject(
                eq("http://localhost:8000/query"),
                any(Map.class),
                eq(Map.class)
        )).thenReturn(aiResponse);
        when(productRepository.findProductsWithAvgStarAndLikeByIds(productIds, memberId, pageable))
                .thenReturn(productPage);

        // when
        Page<ProductSummaryResponse> result = productService.aiSearchProducts(query, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productName()).isEqualTo("제주도 여행");
        assertThat(result.getContent().get(0).averageReviewStar()).isEqualTo(4.5);
        assertThat(result.getContent().get(0).isLiked()).isTrue();
    }

    @Test
    @DisplayName("AI 상품 검색 시 null 쿼리로 예외가 발생한다")
    void aiSearchProducts_NullQuery_ThrowsException() {
        // given
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(memberId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> productService.aiSearchProducts(null, memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("AI 상품 검색 시 빈 쿼리로 예외가 발생한다")
    void aiSearchProducts_EmptyQuery_ThrowsException() {
        // given
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(memberId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> productService.aiSearchProducts("", memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("AI 상품 검색 시 존재하지 않는 회원으로 예외가 발생한다")
    void aiSearchProducts_MemberNotFound_ThrowsException() {
        // given
        String query = "제주도 여행";
        Long nonExistentMemberId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(nonExistentMemberId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> productService.aiSearchProducts(query, nonExistentMemberId, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("AI 상품 검색 시 null memberId로 예외가 발생한다")
    void aiSearchProducts_NullMemberId_ThrowsException() {
        Pageable pageable = PageRequest.of(0, 10);
        assertThatThrownBy(() -> productService.aiSearchProducts("제주도", null, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("AI 상품 검색 - FastAPI가 준 ID 순서를 보존한다")
    void aiSearchProducts_PreservesOrderFromAI() {
        String query = "제주도 여행";
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // AI가 [2, 1] 순서로 결과를 줬다고 가정
        java.util.List<Long> productIds = java.util.List.of(2L, 1L);

        // 리포지토리는 [1, 2] 순서로 반환한다고 가정 (서비스에서 AI 순서로 재정렬해야 함)
        Product product1 = Product.builder().id(1L).productName("P1").description("d").thumbnailImageUrl(null).member(member).country(country).build();
        Product product2 = Product.builder().id(2L).productName("P2").description("d").thumbnailImageUrl(null).member(member).country(country).build();

        ProductWithAvgStarAndLike d1 = ProductWithAvgStarAndLike.builder().product(product1).avgStar(4.0).isLiked(false).build();
        ProductWithAvgStarAndLike d2 = ProductWithAvgStarAndLike.builder().product(product2).avgStar(5.0).isLiked(true).build();
        Page<ProductWithAvgStarAndLike> repoPage = new PageImpl<>(java.util.List.of(d1, d2), pageable, 2);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(restTemplate.postForObject(
                eq("http://localhost:8000/query"), any(Map.class), eq(Map.class)
        )).thenReturn(java.util.Map.of("product_ids", productIds));
        when(productRepository.findProductsWithAvgStarAndLikeByIds(productIds, memberId, pageable))
                .thenReturn(repoPage);

        Page<ProductSummaryResponse> result = productService.aiSearchProducts(query, memberId, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).productId()).isEqualTo(2L);
        assertThat(result.getContent().get(1).productId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("AI 상품 검색 시 AI 서비스 응답이 null이면 빈 결과를 반환한다")
    void aiSearchProducts_NullAiResponse_ReturnsEmpty() {
        // given
        String query = "제주도 여행";
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(memberId)).thenReturn(true);

        // when
        Page<ProductSummaryResponse> result = productService.aiSearchProducts(query, memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("AI 상품 검색 시 AI 서비스에서 product_ids가 없으면 빈 결과를 반환한다")
    void aiSearchProducts_NoProductIds_ReturnsEmpty() {
        // given
        String query = "제주도 여행";
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(memberId)).thenReturn(true);

        // when
        Page<ProductSummaryResponse> result = productService.aiSearchProducts(query, memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("상품 검색 시 존재하지 않는 국가로 검색하면 예외가 발생한다")
    void searchProducts_InvalidCountry_ThrowsException() {
        // given
        String keyword = "제주도";
        String invalidCountryName = "존재하지 않는 국가";
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(countryRepository.existsByName(invalidCountryName)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> productService.searchProducts(keyword, invalidCountryName, memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUNTRY_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 검색 시 null memberId로 예외가 발생한다")
    void searchProducts_NullMemberId_ThrowsException() {
        // given
        String keyword = "제주도";
        String countryName = "전체";
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.searchProducts(keyword, countryName, null, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 상세 조회 시 null memberId로 예외가 발생한다")
    void getProductDetail_NullMemberId_ThrowsException() {
        // given
        Long productId = product.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(productId, null, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 상세 조회 시 null productId로 예외가 발생한다")
    void getProductDetail_NullProductId_ThrowsException() {
        // given
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(memberId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(null, memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("AI 상품 검색 시 AI 서비스 예외가 발생하면 빈 결과를 반환한다")
    void aiSearchProducts_AiServiceException_ReturnsEmpty() {
        // given
        String query = "제주도 여행";
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(restTemplate.postForObject(
                eq("http://localhost:8000/query"),
                any(Map.class),
                eq(Map.class)
        )).thenThrow(new RuntimeException("AI 서비스 오류"));

        // when
        Page<ProductSummaryResponse> result = productService.aiSearchProducts(query, memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("유효한 국가명이면 existsByName=true → 예외 없이 통과(분기 false 커버)")
    void searchProducts_ValidCountry_PassesValidation() {
        // given
        String keyword = "서울";
        String countryName = "대한민국"; // '전체'가 아님 → validateCountry 내부 if 실행
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // existsByName=true로 만들어 if(!existsByName) false 분기 커버
        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(countryRepository.existsByName(countryName)).thenReturn(true);

        ProductWithAvgStarAndLike dto = mock(ProductWithAvgStarAndLike.class);
        when(dto.getProduct()).thenReturn(product);
        when(dto.getAvgStar()).thenReturn(4.5);
        when(dto.getIsLiked()).thenReturn(true);

        Page<ProductWithAvgStarAndLike> page = new PageImpl<>(List.of(dto), pageable, 1);
        when(productRepository.searchProductsWithAvgStarAndLike(keyword, countryName, memberId, pageable))
                .thenReturn(page);

        // when
        Page<ProductSummaryResponse> result =
                productService.searchProducts(keyword, countryName, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productName()).isEqualTo("제주도 여행");
    }

    @Test
    @DisplayName("AI 응답에 product_ids 키가 없으면 빈 결과(OR의 두번째 피연산자 분기 커버)")
    void aiSearchProducts_ResponseWithoutProductIds_ReturnsEmpty() {
        // given
        String query = "제주도";
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        // response != null 이면서 product_ids 키 없음 → (response == null || !containsKey) 조건의 우측 분기 실행
        when(restTemplate.postForObject(eq("http://localhost:8000/query"), any(Map.class), eq(Map.class)))
                .thenReturn(java.util.Collections.emptyMap());

        // when
        Page<ProductSummaryResponse> result = productService.aiSearchProducts(query, memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("AI 응답의 product_ids가 List가 아니면 빈 결과( instanceof false 분기 + return 커버 )")
    void aiSearchProducts_ProductIdsNotAList_ReturnsEmpty() {
        // given
        String query = "제주도";
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.existsById(memberId)).thenReturn(true);
        // product_ids 값이 List가 아닌 케이스 → (productIdsObj instanceof List<?>) 가 false
        when(restTemplate.postForObject(eq("http://localhost:8000/query"), any(Map.class), eq(Map.class)))
                .thenReturn(java.util.Map.of("product_ids", "not-a-list"));

        // when
        Page<ProductSummaryResponse> result = productService.aiSearchProducts(query, memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

}
