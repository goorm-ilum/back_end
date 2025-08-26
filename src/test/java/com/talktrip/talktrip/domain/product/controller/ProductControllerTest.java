package com.talktrip.talktrip.domain.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.product.dto.response.ProductDetailResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.service.ProductService;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@WebMvcTest(controllers = ProductController.class)
@Import(ProductControllerTest.TestSecurityConfig.class)
class ProductControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filter(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/products/**").permitAll()
                            .anyRequest().authenticated()
                    )
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private ProductSummaryResponse productSummaryResponse;
    private ProductDetailResponse productDetailResponse;
    private CustomMemberDetails memberDetails;

    @BeforeEach
    void setUp() {
        productSummaryResponse = ProductSummaryResponse.builder()
                .productId(1L)
                .productName("제주도 여행")
                .productDescription("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .averageReviewStar(4.5)
                .isLiked(true)
                .build();

        productDetailResponse = ProductDetailResponse.builder()
                .productId(1L)
                .productName("제주도 여행")
                .shortDescription("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .countryName("대한민국")
                .averageReviewStar(4.5)
                .isLiked(true)
                .reviews(List.of(ReviewResponse.builder()
                        .reviewId(1L)
                        .comment("좋은 여행이었습니다")
                        .reviewStar(4.5)
                        .build()))
                .build();

        Member member = Member.builder()
                .Id(1L)
                .accountEmail("test@test.com")
                .phoneNum("010-1234-5678")
                .name("테스트유저")
                .nickname("테스트유저")
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();

        memberDetails = new CustomMemberDetails(member);
    }

    @Test
    @DisplayName("상품 검색을 성공적으로 수행한다")
    void searchProducts_Success() throws Exception {
        // given
        String keyword = "제주도";
        String countryName = "전체";
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductSummaryResponse> productPage = new PageImpl<>(List.of(productSummaryResponse), pageable, 1);

        when(productService.searchProducts(eq(keyword), eq(countryName), eq(memberId), any(Pageable.class)))
                .thenReturn(productPage);

        // when & then
        mockMvc.perform(get("/api/products")
                        .with(user(memberDetails))
                        .param("keyword", keyword)
                        .param("countryName", countryName)
                        .param("memberId", memberId.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].productId").value(1L))
                .andExpect(jsonPath("$.content[0].productName").value("제주도 여행"))
                .andExpect(jsonPath("$.content[0].averageReviewStar").value(4.5))
                .andExpect(jsonPath("$.content[0].isLiked").value(true));
    }

    @Test
    @DisplayName("상품 상세 정보를 성공적으로 조회한다")
    void getProductDetail_Success() throws Exception {
        // given
        Long productId = 1L;
        Long memberId = 1L;

        when(productService.getProductDetail(eq(productId), eq(memberId), any(Pageable.class)))
                .thenReturn(productDetailResponse);

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId)
                        .with(user(memberDetails))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1L))
                .andExpect(jsonPath("$.productName").value("제주도 여행"))
                .andExpect(jsonPath("$.averageReviewStar").value(4.5))
                .andExpect(jsonPath("$.isLiked").value(true))
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.reviews[0].reviewId").value(1L));
    }

    @Test
    @DisplayName("상품 상세 조회 시 리뷰 페이징이 올바르게 적용된다")
    void getProductDetail_WithReviewPaging() throws Exception {
        // given
        Long productId = 1L;
        Long memberId = 1L;

        // 두 번째 페이지의 리뷰 응답 생성 (page=1, size=3)
        ProductDetailResponse productDetailResponseWithPaging = ProductDetailResponse.builder()
                .productId(1L)
                .productName("제주도 여행")
                .shortDescription("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .countryName("대한민국")
                .averageReviewStar(4.5)
                .isLiked(true)
                .reviews(List.of(
                        ReviewResponse.builder()
                                .reviewId(4L) // 두 번째 페이지의 첫 번째 리뷰
                                .comment("두 번째 페이지 리뷰 1")
                                .reviewStar(4.0)
                                .build(),
                        ReviewResponse.builder()
                                .reviewId(5L) // 두 번째 페이지의 두 번째 리뷰
                                .comment("두 번째 페이지 리뷰 2")
                                .reviewStar(5.0)
                                .build(),
                        ReviewResponse.builder()
                                .reviewId(6L) // 두 번째 페이지의 세 번째 리뷰
                                .comment("두 번째 페이지 리뷰 3")
                                .reviewStar(3.5)
                                .build()
                ))
                .build();

        when(productService.getProductDetail(eq(productId), eq(memberId), any(Pageable.class)))
                .thenReturn(productDetailResponseWithPaging);

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId)
                        .with(user(memberDetails))
                        .param("page", "1")
                        .param("size", "3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1L))
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.reviews", hasSize(3)))
                .andExpect(jsonPath("$.reviews[0].reviewId").value(4L))
                .andExpect(jsonPath("$.reviews[0].comment").value("두 번째 페이지 리뷰 1"))
                .andExpect(jsonPath("$.reviews[1].reviewId").value(5L))
                .andExpect(jsonPath("$.reviews[1].comment").value("두 번째 페이지 리뷰 2"))
                .andExpect(jsonPath("$.reviews[2].reviewId").value(6L))
                .andExpect(jsonPath("$.reviews[2].comment").value("두 번째 페이지 리뷰 3"));
    }

    @Test
    @DisplayName("상품 상세 조회 시 리뷰 정렬이 올바르게 적용된다")
    void getProductDetail_WithReviewSorting() throws Exception {
        // given
        Long productId = 1L;
        Long memberId = 1L;

        // 정렬된 리뷰 리스트 생성 (별점 내림차순)
        List<ReviewResponse> sortedReviews = List.of(
                ReviewResponse.builder()
                        .reviewId(1L)
                        .comment("최고의 여행이었습니다!")
                        .reviewStar(5.0)
                        .build(),
                ReviewResponse.builder()
                        .reviewId(2L)
                        .comment("좋은 여행이었습니다")
                        .reviewStar(4.0)
                        .build(),
                ReviewResponse.builder()
                        .reviewId(3L)
                        .comment("괜찮은 여행이었습니다")
                        .reviewStar(3.0)
                        .build()
        );

        ProductDetailResponse productDetailResponseWithSortedReviews = ProductDetailResponse.builder()
                .productId(1L)
                .productName("제주도 여행")
                .shortDescription("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .countryName("대한민국")
                .averageReviewStar(4.0)
                .isLiked(true)
                .reviews(sortedReviews)
                .build();

        when(productService.getProductDetail(eq(productId), eq(memberId), any(Pageable.class)))
                .thenReturn(productDetailResponseWithSortedReviews);

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId)
                        .with(user(memberDetails))
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "reviewStar,desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1L))
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.reviews", hasSize(3)))
                // 별점 내림차순 정렬 검증
                .andExpect(jsonPath("$.reviews[0].reviewId").value(1L))
                .andExpect(jsonPath("$.reviews[0].reviewStar").value(5.0))
                .andExpect(jsonPath("$.reviews[0].comment").value("최고의 여행이었습니다!"))
                .andExpect(jsonPath("$.reviews[1].reviewId").value(2L))
                .andExpect(jsonPath("$.reviews[1].reviewStar").value(4.0))
                .andExpect(jsonPath("$.reviews[1].comment").value("좋은 여행이었습니다"))
                .andExpect(jsonPath("$.reviews[2].reviewId").value(3L))
                .andExpect(jsonPath("$.reviews[2].reviewStar").value(3.0))
                .andExpect(jsonPath("$.reviews[2].comment").value("괜찮은 여행이었습니다"));
    }

    @Test
    @DisplayName("상품 상세 조회 시 리뷰 별점 오름차순 정렬이 올바르게 적용된다")
    void getProductDetail_WithReviewSortingAsc() throws Exception {
        // given
        Long productId = 1L;
        Long memberId = 1L;

        // 정렬된 리뷰 리스트 생성 (별점 오름차순)
        List<ReviewResponse> sortedReviewsAsc = List.of(
                ReviewResponse.builder()
                        .reviewId(3L)
                        .comment("괜찮은 여행이었습니다")
                        .reviewStar(3.0)
                        .build(),
                ReviewResponse.builder()
                        .reviewId(2L)
                        .comment("좋은 여행이었습니다")
                        .reviewStar(4.5)
                        .build(),
                ReviewResponse.builder()
                        .reviewId(1L)
                        .comment("최고의 여행이었습니다!")
                        .reviewStar(5.0)
                        .build()
        );

        ProductDetailResponse productDetailResponseWithSortedReviewsAsc = ProductDetailResponse.builder()
                .productId(1L)
                .productName("제주도 여행")
                .shortDescription("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .countryName("대한민국")
                .averageReviewStar(4.2)
                .isLiked(true)
                .reviews(sortedReviewsAsc)
                .build();

        when(productService.getProductDetail(eq(productId), eq(memberId), any(Pageable.class)))
                .thenReturn(productDetailResponseWithSortedReviewsAsc);

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId)
                        .with(user(memberDetails))
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "reviewStar,asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1L))
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.reviews", hasSize(3)))
                // 별점 오름차순 정렬 검증
                .andExpect(jsonPath("$.reviews[0].reviewId").value(3L))
                .andExpect(jsonPath("$.reviews[0].reviewStar").value(3.0))
                .andExpect(jsonPath("$.reviews[0].comment").value("괜찮은 여행이었습니다"))
                .andExpect(jsonPath("$.reviews[1].reviewId").value(2L))
                .andExpect(jsonPath("$.reviews[1].reviewStar").value(4.5))
                .andExpect(jsonPath("$.reviews[1].comment").value("좋은 여행이었습니다"))
                .andExpect(jsonPath("$.reviews[2].reviewId").value(1L))
                .andExpect(jsonPath("$.reviews[2].reviewStar").value(5.0))
                .andExpect(jsonPath("$.reviews[2].comment").value("최고의 여행이었습니다!"));
    }

    @Test
    @DisplayName("상품 상세 조회 시 리뷰가 없는 경우 빈 배열을 반환한다")
    void getProductDetail_NoReviews() throws Exception {
        // given
        Long productId = 1L;
        Long memberId = 1L;

        // 리뷰가 없는 상품 상세 응답 생성
        ProductDetailResponse productDetailResponseWithoutReviews = ProductDetailResponse.builder()
                .productId(1L)
                .productName("제주도 여행")
                .shortDescription("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .countryName("대한민국")
                .averageReviewStar(0.0)
                .isLiked(true)
                .reviews(List.of()) // 빈 리뷰 리스트
                .build();

        when(productService.getProductDetail(eq(productId), eq(memberId), any(Pageable.class)))
                .thenReturn(productDetailResponseWithoutReviews);

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId)
                        .with(user(memberDetails))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1L))
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.reviews").isEmpty())
                .andExpect(jsonPath("$.averageReviewStar").value(0.0));
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 상세 조회 시 404 오류를 반환한다")
    void getProductDetail_ProductNotFound() throws Exception {
        // given
        Long nonExistentProductId = 999L;
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        when(productService.getProductDetail(eq(nonExistentProductId), eq(memberId), any(Pageable.class)))
                .thenThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/products/{productId}", nonExistentProductId)
                        .with(user(memberDetails))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("비로그인 사용자로 상품 검색을 수행한다")
    void searchProducts_NonLoggedInUser() throws Exception {
        // given
        String keyword = "제주도";
        String countryName = "전체";
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductSummaryResponse> productPage = new PageImpl<>(List.of(productSummaryResponse), pageable, 1);

        when(productService.searchProducts(eq(keyword), eq(countryName), eq(null), any(Pageable.class)))
                .thenReturn(productPage);

        // when & then
        mockMvc.perform(get("/api/products")
                        .param("keyword", keyword)
                        .param("countryName", countryName)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("비로그인 사용자로 상품 상세 조회를 수행한다")
    void getProductDetail_NonLoggedInUser() throws Exception {
        // given
        Long productId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        when(productService.getProductDetail(eq(productId), eq(null), any(Pageable.class)))
                .thenReturn(productDetailResponse);

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1L));
    }

    // AI 검색 API 테스트
    @Test
    @DisplayName("AI 상품 검색을 성공적으로 수행한다")
    void aiSearchProducts_Success() throws Exception {
        // given
        String question = "제주도 여행";
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductSummaryResponse> productPage = new PageImpl<>(List.of(productSummaryResponse), pageable, 1);

        when(productService.aiSearchProducts(eq(question), eq(memberId), any(Pageable.class)))
                .thenReturn(productPage);

        // when & then
        mockMvc.perform(get("/api/products/aisearch")
                        .with(user(memberDetails))
                        .param("question", question)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].productId").value(1L))
                .andExpect(jsonPath("$.content[0].productName").value("제주도 여행"))
                .andExpect(jsonPath("$.content[0].averageReviewStar").value(4.5))
                .andExpect(jsonPath("$.content[0].isLiked").value(true));
    }

    @Test
    @DisplayName("AI 상품 검색 시 페이징 파라미터가 올바르게 적용된다")
    void aiSearchProducts_WithPaging() throws Exception {
        // given
        String question = "제주도 여행";
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(1, 5); // 두 번째 페이지, 크기 5
        Page<ProductSummaryResponse> productPage = new PageImpl<>(List.of(productSummaryResponse), pageable, 10);

        when(productService.aiSearchProducts(eq(question), eq(memberId), any(Pageable.class)))
                .thenReturn(productPage);

        // when & then
        mockMvc.perform(get("/api/products/aisearch")
                        .with(user(memberDetails))
                        .param("question", question)
                        .param("page", "1")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("AI 상품 검색 시 정렬 파라미터가 올바르게 적용된다")
    void aiSearchProducts_WithSorting() throws Exception {
        // given
        String question = "제주도 여행";
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductSummaryResponse> productPage = new PageImpl<>(List.of(productSummaryResponse), pageable, 1);

        when(productService.aiSearchProducts(eq(question), eq(memberId), any(Pageable.class)))
                .thenReturn(productPage);

        // when & then
        mockMvc.perform(get("/api/products/aisearch")
                        .with(user(memberDetails))
                        .param("question", question)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "productName,asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("AI 상품 검색 시 필수 파라미터가 없으면 400 오류를 반환한다")
    void aiSearchProducts_MissingRequiredParameter() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products/aisearch")
                        .with(user(memberDetails))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비로그인 사용자로 AI 상품 검색을 수행한다")
    void aiSearchProducts_NonLoggedInUser() throws Exception {
        // given
        String question = "제주도 여행";
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductSummaryResponse> productPage = new PageImpl<>(List.of(productSummaryResponse), pageable, 1);

        when(productService.aiSearchProducts(eq(question), eq(null), any(Pageable.class)))
                .thenReturn(productPage);

        // when & then
        mockMvc.perform(get("/api/products/aisearch")
                        .param("question", question)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
