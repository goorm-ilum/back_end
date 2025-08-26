package com.talktrip.talktrip.domain.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.review.dto.request.ReviewRequest;
import com.talktrip.talktrip.domain.review.dto.response.MyReviewFormResponse;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;
import com.talktrip.talktrip.domain.review.service.ReviewService;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ReviewException;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewController.class)
@Import(ReviewControllerTest.TestSecurityConfig.class)
class ReviewControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filter(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/me/**", "/api/orders/**", "/api/reviews/**", "/api/admin/**").authenticated()
                            .anyRequest().permitAll()
                    )
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .exceptionHandling(exceptionHandling -> exceptionHandling
                            .authenticationEntryPoint((request, response, authException) -> {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            })
                    )
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    private ReviewRequest reviewRequest;
    private ReviewResponse reviewResponse;
    private MyReviewFormResponse myReviewFormResponse;
    private CustomMemberDetails memberDetails;
    private CustomMemberDetails sellerDetails;

    @BeforeEach
    void setUp() {
        Member member = Member.builder()
                .Id(1L)
                .accountEmail("test@test.com")
                .phoneNum("010-1234-5678")
                .name("테스트유저")
                .nickname("테스트유저")
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();

        Member seller = Member.builder()
                .Id(2L)
                .accountEmail("seller@test.com")
                .phoneNum("010-9999-0000")
                .name("판매자")
                .nickname("셀러")
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();

        memberDetails = new CustomMemberDetails(member);
        sellerDetails = new CustomMemberDetails(seller);

        reviewRequest = ReviewRequest.builder()
                .comment("좋은 여행이었습니다")
                .reviewStar(4.5D)
                .build();

        reviewResponse = ReviewResponse.builder()
                .reviewId(1L)
                .nickName("테스트유저")
                .productName("제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .comment("좋은 여행이었습니다")
                .reviewStar(4.5D)
                .updatedAt("2024-01-01T00:00:00")
                .build();

        myReviewFormResponse = MyReviewFormResponse.builder()
                .reviewId(null)
                .productName("제주도 여행")
                .thumbnailUrl("https://example.com/jeju.jpg")
                .myStar(null)
                .myComment(null)
                .build();
    }

    @Test
    @DisplayName("리뷰를 성공적으로 생성한다")
    void createReview_Success() throws Exception {
        Long orderId = 1L;

        doNothing().when(reviewService).createReview(orderId, memberDetails.getId(), reviewRequest);

        mockMvc.perform(post("/api/orders/{orderId}/review", orderId)
                        .with(user(memberDetails))
                        .content(objectMapper.writeValueAsString(reviewRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("존재하지 않는 주문으로 리뷰 생성 시 404 오류를 반환한다")
    void createReview_OrderNotFound() throws Exception {
        Long nonExistentOrderId = 999L;

        doThrow(new ReviewException(ErrorCode.ORDER_NOT_FOUND))
                .when(reviewService).createReview(nonExistentOrderId, memberDetails.getId(), reviewRequest);

        mockMvc.perform(post("/api/orders/{orderId}/review", nonExistentOrderId)
                        .with(user(memberDetails))
                        .content(objectMapper.writeValueAsString(reviewRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("리뷰를 성공적으로 수정한다")
    void updateReview_Success() throws Exception {
        Long reviewId = 1L;
        ReviewRequest updateRequest = ReviewRequest.builder()
                .comment("수정된 리뷰입니다 10글자 이상")
                .reviewStar(5.0D)
                .build();

        doNothing().when(reviewService).updateReview(reviewId, memberDetails.getId(), updateRequest);

        mockMvc.perform(put("/api/reviews/{reviewId}", reviewId)
                        .with(user(memberDetails))
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 수정 시 404 오류를 반환한다")
    void updateReview_ReviewNotFound() throws Exception {
        Long nonExistentReviewId = 999L;

        doThrow(new ReviewException(ErrorCode.REVIEW_NOT_FOUND))
                .when(reviewService).updateReview(nonExistentReviewId, memberDetails.getId(), reviewRequest);

        mockMvc.perform(put("/api/reviews/{reviewId}", nonExistentReviewId)
                        .with(user(memberDetails))
                        .content(objectMapper.writeValueAsString(reviewRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("REVIEW_NOT_FOUND"));
    }

    @Test
    @DisplayName("리뷰를 성공적으로 삭제한다")
    void deleteReview_Success() throws Exception {
        Long reviewId = 1L;

        doNothing().when(reviewService).deleteReview(reviewId, memberDetails.getId());

        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                        .with(user(memberDetails)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 삭제 시 404 오류를 반환한다")
    void deleteReview_ReviewNotFound() throws Exception {
        Long nonExistentReviewId = 999L;

        doThrow(new ReviewException(ErrorCode.REVIEW_NOT_FOUND))
                .when(reviewService).deleteReview(nonExistentReviewId, memberDetails.getId());

        mockMvc.perform(delete("/api/reviews/{reviewId}", nonExistentReviewId)
                        .with(user(memberDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("REVIEW_NOT_FOUND"));
    }

    @Test
    @DisplayName("내 리뷰 목록을 성공적으로 조회한다")
    void getMyReviews_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 9, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ReviewResponse> reviewPage = new PageImpl<>(List.of(reviewResponse), pageable, 1);

        when(reviewService.getMyReviews(memberDetails.getId(), pageable)).thenReturn(reviewPage);

        mockMvc.perform(get("/api/me/reviews")
                        .with(user(memberDetails))
                        .param("page", "0")
                        .param("size", "9")
                        .param("sort", "updatedAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].reviewId").value(1L));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 리뷰 목록 조회 시 404 오류를 반환한다")
    void getMyReviews_UserNotFound() throws Exception {
        when(reviewService.getMyReviews(memberDetails.getId(), PageRequest.of(0, 9, Sort.by(Sort.Direction.DESC, "updatedAt"))))
                .thenThrow(new ReviewException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/me/reviews")
                        .with(user(memberDetails))
                        .param("page", "0")
                        .param("size", "9")
                        .param("sort", "updatedAt,desc"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("리뷰 생성 폼을 성공적으로 조회한다")
    void getReviewCreateForm_Success() throws Exception {
        Long orderId = 1L;

        when(reviewService.getReviewCreateForm(orderId, memberDetails.getId()))
                .thenReturn(myReviewFormResponse);

        mockMvc.perform(get("/api/orders/{orderId}/review/form", orderId)
                        .with(user(memberDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("제주도 여행"));
    }

    @Test
    @DisplayName("리뷰 수정 폼을 성공적으로 조회한다")
    void getReviewUpdateForm_Success() throws Exception {
        Long reviewId = 1L;
        MyReviewFormResponse updateFormResponse = MyReviewFormResponse.builder()
                .reviewId(1L)
                .productName("제주도 여행")
                .thumbnailUrl("https://example.com/jeju.jpg")
                .myStar(4.5)
                .myComment("기존 리뷰 내용")
                .build();

        when(reviewService.getReviewUpdateForm(reviewId, memberDetails.getId()))
                .thenReturn(updateFormResponse);

        mockMvc.perform(get("/api/reviews/{reviewId}/form", reviewId)
                        .with(user(memberDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(1L));
    }

    @Test
    @DisplayName("관리자: 특정 상품의 리뷰 목록을 성공적으로 조회한다")
    void getReviewsForSellerProduct_Success() throws Exception {
        Long productId = 1L;
        Pageable pageable = PageRequest.of(0, 10,Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ReviewResponse> reviewPage = new PageImpl<>(List.of(reviewResponse), pageable, 1);

        when(reviewService.getReviewsForAdminProduct(sellerDetails.getId(), productId, pageable))
                .thenReturn(reviewPage);

        mockMvc.perform(get("/api/admin/products/{productId}/reviews", productId)
                        .with(user(sellerDetails))
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "updatedAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].reviewId").value(1L));
    }

    @Test
    @DisplayName("리뷰 생성 시 유효하지 않은 데이터로 400 오류를 반환한다")
    void createReview_InvalidData() throws Exception {
        Long orderId = 1L;
        ReviewRequest invalidRequest = ReviewRequest.builder()
                .comment("")
                .reviewStar(6.0D)
                .build();

        mockMvc.perform(post("/api/orders/{orderId}/review", orderId)
                        .with(user(memberDetails))
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("리뷰 수정 시 유효하지 않은 데이터로 400 오류를 반환한다")
    void updateReview_InvalidData_ThrowsValidationException() throws Exception {
        Long reviewId = 1L;
        ReviewRequest invalidRequest = ReviewRequest.builder()
                .comment("")
                .reviewStar(0.0D)
                .build();

        mockMvc.perform(put("/api/reviews/{reviewId}", reviewId)
                        .with(user(memberDetails))
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비로그인 사용자로 리뷰 생성 시 401 오류를 반환한다")
    void createReview_Unauthorized() throws Exception {
        Long orderId = 1L;

        mockMvc.perform(post("/api/orders/{orderId}/review", orderId)
                        .content(objectMapper.writeValueAsString(reviewRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비로그인 사용자로 리뷰 수정 시 401 오류를 반환한다")
    void updateReview_Unauthorized() throws Exception {
        Long reviewId = 1L;

        mockMvc.perform(put("/api/reviews/{reviewId}", reviewId)
                        .content(objectMapper.writeValueAsString(reviewRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비로그인 사용자로 리뷰 삭제 시 401 오류를 반환한다")
    void deleteReview_Unauthorized() throws Exception {
        Long reviewId = 1L;

        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비로그인 사용자로 내 리뷰 목록 조회 시 401 오류를 반환한다")
    void getMyReviews_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/me/reviews")
                        .param("page", "0")
                        .param("size", "9"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비로그인 사용자로 리뷰 생성 폼 조회 시 401 오류를 반환한다")
    void getReviewCreateForm_Unauthorized() throws Exception {
        Long orderId = 1L;

        mockMvc.perform(get("/api/orders/{orderId}/review/form", orderId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비로그인 사용자로 리뷰 수정 폼 조회 시 401 오류를 반환한다")
    void getReviewUpdateForm_Unauthorized() throws Exception {
        Long reviewId = 1L;

        mockMvc.perform(get("/api/reviews/{reviewId}/form", reviewId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비로그인 사용자로 관리자 상품 리뷰 조회 시 401 오류를 반환한다")
    void getReviewsForSellerProduct_Unauthorized() throws Exception {
        Long productId = 1L;

        mockMvc.perform(get("/api/admin/products/{productId}/reviews", productId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리뷰 생성 시 null 데이터로 400 오류를 반환한다")
    void createReview_NullData() throws Exception {
        Long orderId = 1L;

        mockMvc.perform(post("/api/orders/{orderId}/review", orderId)
                        .with(user(memberDetails))
                        .content("null")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("리뷰 수정 시 null 데이터로 400 오류를 반환한다")
    void updateReview_NullData() throws Exception {
        Long reviewId = 1L;

        mockMvc.perform(put("/api/reviews/{reviewId}", reviewId)
                        .with(user(memberDetails))
                        .content("null")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
