package com.talktrip.talktrip.domain.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.review.dto.request.ReviewRequest;
import com.talktrip.talktrip.domain.review.dto.response.MyReviewFormResponse;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;
import com.talktrip.talktrip.domain.review.service.ReviewService;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ReviewException;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(controllers = ReviewController.class)
@Import(ReviewControllerTest.TestConfig.class)
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ReviewService reviewService;
    @Autowired ObjectMapper om;

    private final ReviewResponse REVIEW_DTO = new ReviewResponse(
            REVIEW_ID, "user", PRODUCT_NAME_1, null, COMMENT_TEST, STAR_4_0, TIME
    );

    @TestConfiguration
    static class TestConfig {
        @Bean ReviewService reviewService() { return mock(ReviewService.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }

    @AfterEach void resetMocks() { reset(reviewService); }

    private UsernamePasswordAuthenticationToken authUser() {
        CustomMemberDetails md = mock(CustomMemberDetails.class);
        given(md.getId()).willReturn(USER_ID);
        return new UsernamePasswordAuthenticationToken(
                md, null, List.of(new SimpleGrantedAuthority(ROLE_USER)));
    }

    @Nested @DisplayName("POST " + EP_CREATE_REVIEW + " (리뷰 작성)")
    class CreateReview {
        @Test @DisplayName("201 Created + 서비스 전달값 검증")
        void create_ok() throws Exception {
            willDoNothing().given(reviewService)
                    .createReview(eq(ORDER_ID), eq(USER_ID), any(ReviewRequest.class));

            ReviewRequest req = new ReviewRequest(COMMENT_TEST, STAR_4_5);

            mockMvc.perform(post(EP_CREATE_REVIEW, ORDER_ID)
                            .with(authentication(authUser())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            ArgumentCaptor<ReviewRequest> cap = ArgumentCaptor.forClass(ReviewRequest.class);
            then(reviewService).should().createReview(eq(ORDER_ID), eq(USER_ID), cap.capture());
            assertThat(cap.getValue().comment()).isEqualTo(COMMENT_TEST);
            assertThat(cap.getValue().reviewStar()).isEqualTo(STAR_4_5);
        }

        @Test @DisplayName("400 ORDER_EMPTY")
        void create_orderEmpty() throws Exception {
            willThrow(new ReviewException(ErrorCode.ORDER_EMPTY))
                    .given(reviewService).createReview(eq(ORDER_ID), eq(USER_ID), any(ReviewRequest.class));

            mockMvc.perform(post(EP_CREATE_REVIEW, ORDER_ID)
                            .with(authentication(authUser())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new ReviewRequest(COMMENT_TEST, STAR_4_0))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ORDER_EMPTY));
        }

        @Test @DisplayName("400 ORDER_NOT_COMPLETED")
        void create_orderNotCompleted() throws Exception {
            willThrow(new ReviewException(ErrorCode.ORDER_NOT_COMPLETED))
                    .given(reviewService).createReview(eq(ORDER_ID), eq(USER_ID), any(ReviewRequest.class));

            mockMvc.perform(post(EP_CREATE_REVIEW, ORDER_ID)
                            .with(authentication(authUser())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new ReviewRequest(COMMENT_TEST, STAR_4_0))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ORDER_NOT_COMPLETED));
        }

        @Test @DisplayName("401 인증 없음")
        void create_unauth() throws Exception {
            mockMvc.perform(post(EP_CREATE_REVIEW, ORDER_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new ReviewRequest(COMMENT_TEST, STAR_3_0))))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 ACCESS_DENIED (주문 소유자 아님)")
        void create_forbidden() throws Exception {
            willThrow(new ReviewException(ErrorCode.ACCESS_DENIED))
                    .given(reviewService).createReview(eq(ORDER_ID), eq(USER_ID), any());

            mockMvc.perform(post(EP_CREATE_REVIEW, ORDER_ID)
                            .with(authentication(authUser())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new ReviewRequest(COMMENT_TEST, STAR_4_0))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ACCESS_DENIED))
                    .andExpect(jsonPath(JSON_MESSAGE).value(MSG_ACCESS_DENIED));
        }

        @Test @DisplayName("404 PRODUCT_NOT_FOUND")
        void create_productNotFound() throws Exception {
            willThrow(new ReviewException(ErrorCode.PRODUCT_NOT_FOUND))
                    .given(reviewService).createReview(eq(ORDER_ID), eq(USER_ID), any(ReviewRequest.class));

            mockMvc.perform(post(EP_CREATE_REVIEW, ORDER_ID)
                            .with(authentication(authUser())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new ReviewRequest(COMMENT_TEST, STAR_4_0))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_PRODUCT_NOT_FOUND));
        }

        @Test @DisplayName("404 USER_NOT_FOUND")
        void create_userNotFound() throws Exception {
            willThrow(new ReviewException(ErrorCode.USER_NOT_FOUND))
                    .given(reviewService).createReview(eq(ORDER_ID), eq(USER_ID), any(ReviewRequest.class));

            mockMvc.perform(post(EP_CREATE_REVIEW, ORDER_ID)
                            .with(authentication(authUser())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new ReviewRequest(COMMENT_TEST, STAR_4_0))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_USER_NOT_FOUND));
        }

        @Test @DisplayName("404 ORDER_NOT_FOUND")
        void create_orderNotFound() throws Exception {
            willThrow(new ReviewException(ErrorCode.ORDER_NOT_FOUND))
                    .given(reviewService).createReview(eq(ORDER_ID), eq(USER_ID), any(ReviewRequest.class));

            mockMvc.perform(post(EP_CREATE_REVIEW, ORDER_ID)
                            .with(authentication(authUser())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new ReviewRequest(COMMENT_TEST, STAR_4_0))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ORDER_NOT_FOUND));
        }

        @Test @DisplayName("409 ALREADY_REVIEWED")
        void create_alreadyReviewed() throws Exception {
            willThrow(new ReviewException(ErrorCode.ALREADY_REVIEWED))
                    .given(reviewService).createReview(eq(ORDER_ID), eq(USER_ID), any(ReviewRequest.class));

            mockMvc.perform(post(EP_CREATE_REVIEW, ORDER_ID)
                            .with(authentication(authUser())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new ReviewRequest(COMMENT_TEST, STAR_4_0))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ALREADY_REVIEWED));
        }
    }

    @Nested @DisplayName("PUT " + EP_UPDATE_REVIEW + " (리뷰 수정)")
    class UpdateReview {

        @Test @DisplayName("정상 수정 → 200 OK")
        void update_ok() throws Exception {
            mockMvc.perform(put(EP_UPDATE_REVIEW, REVIEW_ID)
                            .with(authentication(authUser())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new ReviewRequest("updated", STAR_5_0))))
                    .andExpect(status().isOk());

            then(reviewService).should().updateReview(eq(REVIEW_ID), eq(USER_ID), any(ReviewRequest.class));
        }

        @Test @DisplayName("401 인증 없음")
        void update_unauth() throws Exception {
            mockMvc.perform(put(EP_UPDATE_REVIEW, REVIEW_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new ReviewRequest("updated", STAR_5_0))))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 ACCESS_DENIED (내 리뷰 아님)")
        void update_forbidden() throws Exception {
            willThrow(new ReviewException(ErrorCode.ACCESS_DENIED))
                    .given(reviewService).updateReview(eq(REVIEW_ID), eq(USER_ID), any(ReviewRequest.class));

            mockMvc.perform(put(EP_UPDATE_REVIEW, REVIEW_ID)
                            .with(authentication(authUser())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new ReviewRequest("updated", STAR_5_0))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ACCESS_DENIED));
        }

        @Test @DisplayName("404 REVIEW_NOT_FOUND")
        void update_notFound() throws Exception {
            willThrow(new ReviewException(ErrorCode.REVIEW_NOT_FOUND))
                    .given(reviewService).updateReview(eq(REVIEW_ID), eq(USER_ID), any(ReviewRequest.class));

            mockMvc.perform(put(EP_UPDATE_REVIEW, REVIEW_ID)
                            .with(authentication(authUser())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(new ReviewRequest("updated", STAR_5_0))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_REVIEW_NOT_FOUND))
                    .andExpect(jsonPath(JSON_MESSAGE).value(MSG_REVIEW_NOT_FOUND));
        }
    }

    @Nested @DisplayName("DELETE " + EP_DELETE_REVIEW + " (리뷰 삭제)")
    class DeleteReview {

        @Test @DisplayName("정상 삭제 → 204 No Content")
        void delete_ok() throws Exception {
            mockMvc.perform(delete(EP_DELETE_REVIEW, REVIEW_ID)
                            .with(authentication(authUser())).with(csrf()))
                    .andExpect(status().isNoContent());

            then(reviewService).should().deleteReview(REVIEW_ID, USER_ID);
        }

        @Test @DisplayName("401 인증 없음")
        void delete_unauth() throws Exception {
            mockMvc.perform(delete(EP_DELETE_REVIEW, REVIEW_ID).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 ACCESS_DENIED (내 리뷰 아님)")
        void delete_forbidden() throws Exception {
            willThrow(new ReviewException(ErrorCode.ACCESS_DENIED))
                    .given(reviewService).deleteReview(REVIEW_ID, USER_ID);

            mockMvc.perform(delete(EP_DELETE_REVIEW, REVIEW_ID)
                            .with(authentication(authUser())).with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ACCESS_DENIED))
                    .andExpect(jsonPath(JSON_MESSAGE).value(MSG_ACCESS_DENIED));
        }

        @Test @DisplayName("404 REVIEW_NOT_FOUND")
        void delete_notFound() throws Exception {
            willThrow(new ReviewException(ErrorCode.REVIEW_NOT_FOUND))
                    .given(reviewService).deleteReview(REVIEW_ID, USER_ID);

            mockMvc.perform(delete(EP_DELETE_REVIEW, REVIEW_ID)
                            .with(authentication(authUser())).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_REVIEW_NOT_FOUND))
                    .andExpect(jsonPath(JSON_MESSAGE).value(MSG_REVIEW_NOT_FOUND));
        }
    }

    @Nested @DisplayName("GET " + EP_GET_MY_REVIEWS + " (내 리뷰 목록)")
    class GetMyReviews {

        @Test @DisplayName("기본 page=0,size=9,sort=updatedAt,desc (빈 페이지)")
        void defaultEmpty() throws Exception {
            Page<ReviewResponse> stub = new PageImpl<>(List.of(), PAGE_0_SIZE_9, 0);
            given(reviewService.getMyReviews(eq(USER_ID), any(Pageable.class))).willReturn(stub);

            mockMvc.perform(get(EP_GET_MY_REVIEWS).with(authentication(authUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_CONTENT_LEN).value(0))
                    .andExpect(jsonPath(JSON_TOTAL_ELEMENTS).value(0));
        }

        @Test @DisplayName("데이터 반환 + 페이징 메타 검증")
        void hasData() throws Exception {
            Page<ReviewResponse> stub = new PageImpl<>(List.of(REVIEW_DTO), PAGE_0_SIZE_9, 1);
            given(reviewService.getMyReviews(eq(USER_ID), any(Pageable.class))).willReturn(stub);

            mockMvc.perform(get(EP_GET_MY_REVIEWS).with(authentication(authUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_CONTENT_0_REVIEW_ID).value(REVIEW_ID))
                    .andExpect(jsonPath(JSON_CONTENT_0_PRODUCT_NAME).value(PRODUCT_NAME_1))
                    .andExpect(jsonPath("$.content[0].comment").value(COMMENT_TEST))
                    .andExpect(jsonPath("$.content[0].reviewStar").value(STAR_4_0))
                    .andExpect(jsonPath(JSON_TOTAL_ELEMENTS).value(1))
                    .andExpect(jsonPath(JSON_NUMBER).value(PAGE_0))
                    .andExpect(jsonPath(JSON_SIZE).value(SIZE_9));
        }

        @Test @DisplayName("초과 페이지 요청 → 빈 페이지 반환")
        void overflowPage_empty() throws Exception {
            Pageable overflow = PageRequest.of(PAGE_7, SIZE_9, DEFAULT_SORT_UPDATED_DESC);
            Page<ReviewResponse> stub = new PageImpl<>(List.of(), overflow, 2);
            given(reviewService.getMyReviews(eq(USER_ID), any(Pageable.class))).willReturn(stub);

            mockMvc.perform(get(EP_GET_MY_REVIEWS)
                            .param("page", String.valueOf(PAGE_7))
                            .with(authentication(authUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_CONTENT_LEN).value(0))
                    .andExpect(jsonPath(JSON_NUMBER).value(PAGE_7))
                    .andExpect(jsonPath(JSON_SIZE).value(SIZE_9))
                    .andExpect(jsonPath(JSON_TOTAL_ELEMENTS).value(2));
        }

        @Test @DisplayName("잘못된 정렬 → 400")
        void invalidSort() throws Exception {
            mockMvc.perform(get(EP_GET_MY_REVIEWS)
                            .param("sort", SORT_UPDATED_AT + ",down")
                            .with(authentication(authUser())))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("401 인증 없음")
        void unauth() throws Exception {
            mockMvc.perform(get(EP_GET_MY_REVIEWS))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("404 USER_NOT_FOUND (인증 OK, DB 미존재)")
        void userNotFound() throws Exception {
            willThrow(new ReviewException(ErrorCode.USER_NOT_FOUND))
                    .given(reviewService).getMyReviews(eq(USER_ID), any(Pageable.class));

            mockMvc.perform(get(EP_GET_MY_REVIEWS).with(authentication(authUser())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_USER_NOT_FOUND))
                    .andExpect(jsonPath(JSON_MESSAGE).value(MSG_USER_NOT_FOUND));
        }
    }

    @Nested @DisplayName("GET " + EP_GET_CREATE_FORM + " (리뷰 작성 폼)")
    class GetCreateForm {

        @Test @DisplayName("200 OK (상품 정보만, 내 리뷰 정보는 null)")
        void createForm_ok() throws Exception {
            MyReviewFormResponse resp = MyReviewFormResponse.builder()
                    .reviewId(null)
                    .productName(PRODUCT_NAME_1)
                    .thumbnailUrl(null)
                    .myStar(null)
                    .myComment(null)
                    .build();

            given(reviewService.getReviewCreateForm(eq(ORDER_ID), eq(USER_ID))).willReturn(resp);

            mockMvc.perform(get(EP_GET_CREATE_FORM, ORDER_ID).with(authentication(authUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PRODUCT_NAME).value(PRODUCT_NAME_1))
                    .andExpect(jsonPath(JSON_REVIEW_ID, nullValue()))
                    .andExpect(jsonPath(JSON_THUMBNAIL_URL, nullValue()))
                    .andExpect(jsonPath(JSON_MY_STAR, nullValue()))
                    .andExpect(jsonPath(JSON_MY_COMMENT, nullValue()));
        }

        @Test @DisplayName("400 ORDER_NOT_COMPLETED")
        void createForm_orderNotCompleted() throws Exception {
            willThrow(new ReviewException(ErrorCode.ORDER_NOT_COMPLETED))
                    .given(reviewService).getReviewCreateForm(eq(ORDER_ID), eq(USER_ID));

            mockMvc.perform(get(EP_GET_CREATE_FORM, ORDER_ID).with(authentication(authUser())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ORDER_NOT_COMPLETED));
        }

        @Test @DisplayName("401 인증 없음")
        void createForm_unauth() throws Exception {
            mockMvc.perform(get(EP_GET_CREATE_FORM, ORDER_ID))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 ACCESS_DENIED")
        void createForm_forbidden() throws Exception {
            willThrow(new ReviewException(ErrorCode.ACCESS_DENIED))
                    .given(reviewService).getReviewCreateForm(eq(ORDER_ID), eq(USER_ID));

            mockMvc.perform(get(EP_GET_CREATE_FORM, ORDER_ID).with(authentication(authUser())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ACCESS_DENIED));
        }

        @Test @DisplayName("404 ORDER_NOT_FOUND")
        void createForm_orderNotFound() throws Exception {
            willThrow(new ReviewException(ErrorCode.ORDER_NOT_FOUND))
                    .given(reviewService).getReviewCreateForm(eq(ORDER_ID), eq(USER_ID));

            mockMvc.perform(get(EP_GET_CREATE_FORM, ORDER_ID).with(authentication(authUser())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ORDER_NOT_FOUND));
        }

        @Test @DisplayName("409 ALREADY_REVIEWED")
        void createForm_alreadyReviewed() throws Exception {
            willThrow(new ReviewException(ErrorCode.ALREADY_REVIEWED))
                    .given(reviewService).getReviewCreateForm(eq(ORDER_ID), eq(USER_ID));

            mockMvc.perform(get(EP_GET_CREATE_FORM, ORDER_ID).with(authentication(authUser())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ALREADY_REVIEWED));
        }
    }

    @Nested @DisplayName("GET " + EP_GET_UPDATE_FORM + " (리뷰 수정 폼)")
    class GetUpdateForm {

        @Test @DisplayName("200 OK (내 리뷰 정보 포함)")
        void updateForm_ok() throws Exception {
            MyReviewFormResponse resp = MyReviewFormResponse.builder()
                    .reviewId(REVIEW_ID)
                    .productName(PRODUCT_NAME_1)
                    .thumbnailUrl(null)
                    .myStar(STAR_4_0)
                    .myComment(COMMENT_TEST)
                    .build();

            given(reviewService.getReviewUpdateForm(eq(REVIEW_ID), eq(USER_ID))).willReturn(resp);

            mockMvc.perform(get(EP_GET_UPDATE_FORM, REVIEW_ID).with(authentication(authUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_REVIEW_ID).value(REVIEW_ID))
                    .andExpect(jsonPath(JSON_PRODUCT_NAME).value(PRODUCT_NAME_1))
                    .andExpect(jsonPath(JSON_THUMBNAIL_URL, nullValue()))
                    .andExpect(jsonPath(JSON_MY_STAR).value(STAR_4_0))
                    .andExpect(jsonPath(JSON_MY_COMMENT).value(COMMENT_TEST));
        }

        @Test @DisplayName("401 인증 없음")
        void updateForm_unauth() throws Exception {
            mockMvc.perform(get(EP_GET_UPDATE_FORM, REVIEW_ID))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 ACCESS_DENIED")
        void updateForm_forbidden() throws Exception {
            willThrow(new ReviewException(ErrorCode.ACCESS_DENIED))
                    .given(reviewService).getReviewUpdateForm(eq(REVIEW_ID), eq(USER_ID));

            mockMvc.perform(get(EP_GET_UPDATE_FORM, REVIEW_ID).with(authentication(authUser())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ACCESS_DENIED));
        }

        @Test @DisplayName("404 REVIEW_NOT_FOUND")
        void updateForm_notFound() throws Exception {
            willThrow(new ReviewException(ErrorCode.REVIEW_NOT_FOUND))
                    .given(reviewService).getReviewUpdateForm(eq(REVIEW_ID), eq(USER_ID));

            mockMvc.perform(get(EP_GET_UPDATE_FORM, REVIEW_ID).with(authentication(authUser())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_REVIEW_NOT_FOUND))
                    .andExpect(jsonPath(JSON_MESSAGE).value(MSG_REVIEW_NOT_FOUND));
        }
    }

    @Nested @DisplayName("GET " + EP_GET_ADMIN_PRODUCT_REVIEWS + " (판매자 상품 리뷰)")
    class GetAdminProductReviews {

        @Test @DisplayName("200 OK")
        void getAdminProductReviews_ok() throws Exception {
            Page<ReviewResponse> stub = new PageImpl<>(List.of(REVIEW_DTO), PAGE_0_SIZE_10, 1);

            given(reviewService.getReviewsForAdminProduct(eq(USER_ID), eq(PRODUCT_ID), any(Pageable.class)))
                    .willReturn(stub);

            mockMvc.perform(get(EP_GET_ADMIN_PRODUCT_REVIEWS, PRODUCT_ID)
                            .with(authentication(authUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_CONTENT_0_REVIEW_ID).value(REVIEW_ID))
                    .andExpect(jsonPath(JSON_TOTAL_ELEMENTS).value(1));
        }

        @Test @DisplayName("잘못된 정렬 → 400")
        void getAdminProductReviews_invalidSortDir_400() throws Exception {
            mockMvc.perform(get(EP_GET_ADMIN_PRODUCT_REVIEWS, PRODUCT_ID)
                            .param("sort", SORT_UPDATED_AT + ",DOWNWARD")
                            .with(authentication(authUser())))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("401 인증 없음")
        void getAdminProductReviews_unauth() throws Exception {
            mockMvc.perform(get(EP_GET_ADMIN_PRODUCT_REVIEWS, PRODUCT_ID))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 ACCESS_DENIED (내 상품 아님)")
        void getAdminProductReviews_forbidden() throws Exception {
            willThrow(new ReviewException(ErrorCode.ACCESS_DENIED))
                    .given(reviewService).getReviewsForAdminProduct(eq(USER_ID), eq(PRODUCT_ID), any(Pageable.class));

            mockMvc.perform(get(EP_GET_ADMIN_PRODUCT_REVIEWS, PRODUCT_ID)
                            .with(authentication(authUser())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ACCESS_DENIED));
        }
    }
}
