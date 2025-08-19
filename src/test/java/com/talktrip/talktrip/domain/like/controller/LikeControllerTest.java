package com.talktrip.talktrip.domain.like.controller;

import com.talktrip.talktrip.domain.like.service.LikeService;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(controllers = LikeController.class)
@Import(LikeControllerTest.TestConfig.class)
class LikeControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired LikeService likeService;

    @TestConfiguration
    static class TestConfig {
        @Bean LikeService likeService() { return mock(LikeService.class); }
    }

    @AfterEach
    void resetMocks() { reset(likeService); }

    private UsernamePasswordAuthenticationToken authUser() {
        CustomMemberDetails md = mock(CustomMemberDetails.class);
        given(md.getId()).willReturn(USER_ID);
        return new UsernamePasswordAuthenticationToken(
                md, null, List.of(new SimpleGrantedAuthority(ROLE_USER)));
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

    private ProductSummaryResponse dtoWithAvg(Product p, float... stars) {
        for (float s : stars) {
            p.getReviews().add(Review.builder().product(p).reviewStar(s).build());
        }
        float avg = (float) p.getReviews().stream()
                .mapToDouble(Review::getReviewStar).average().orElse(STAR_0_0);
        return ProductSummaryResponse.from(p, avg, true);
    }

    @Nested
    @DisplayName("POST " + EP_TOGGLE_LIKE + " (좋아요 토글)")
    class ToggleLike {

        @Test @DisplayName("200 OK + 서비스 전달값 검증")
        void toggleLike_ok() throws Exception {
            willDoNothing().given(likeService).toggleLike(eq(PRODUCT_ID), eq(USER_ID));

            mockMvc.perform(post(EP_TOGGLE_LIKE, PRODUCT_ID)
                            .with(authentication(authUser()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            ArgumentCaptor<Long> productCap = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> userCap = ArgumentCaptor.forClass(Long.class);
            then(likeService).should().toggleLike(productCap.capture(), userCap.capture());
            assertThat(productCap.getValue()).isEqualTo(PRODUCT_ID);
            assertThat(userCap.getValue()).isEqualTo(USER_ID);
        }

        @Test @DisplayName("401 인증 없음")
        void toggleLike_unauth() throws Exception {
            mockMvc.perform(post(EP_TOGGLE_LIKE, PRODUCT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("404 USER_NOT_FOUND")
        void toggleLike_userNotFound() throws Exception {
            willThrow(new MemberException(com.talktrip.talktrip.global.exception.ErrorCode.USER_NOT_FOUND))
                    .given(likeService).toggleLike(eq(PRODUCT_ID), eq(USER_ID));

            mockMvc.perform(post(EP_TOGGLE_LIKE, PRODUCT_ID)
                            .with(authentication(authUser())).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_USER_NOT_FOUND))
                    .andExpect(jsonPath(JSON_MESSAGE).value(MSG_USER_NOT_FOUND));
        }

        @Test @DisplayName("404 PRODUCT_NOT_FOUND")
        void toggleLike_productNotFound() throws Exception {
            willThrow(new ProductException(com.talktrip.talktrip.global.exception.ErrorCode.PRODUCT_NOT_FOUND))
                    .given(likeService).toggleLike(eq(PRODUCT_ID), eq(USER_ID));

            mockMvc.perform(post(EP_TOGGLE_LIKE, PRODUCT_ID)
                            .with(authentication(authUser())).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_PRODUCT_NOT_FOUND))
                    .andExpect(jsonPath(JSON_MESSAGE).value(MSG_PRODUCT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("GET " + EP_GET_MY_LIKES + " (내 좋아요 상품 목록)")
    class GetMyLikes {

        @Test @DisplayName("기본 page=0,size=9,sort=updatedAt,desc (빈 페이지)")
        void getMyLikes_defaultEmpty() throws Exception {
            Page<ProductSummaryResponse> stub = new PageImpl<>(List.of(), PAGE_0_SIZE_9, 0);
            given(likeService.getLikedProducts(eq(USER_ID), any(Pageable.class))).willReturn(stub);

            mockMvc.perform(get(EP_GET_MY_LIKES).with(authentication(authUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_CONTENT_LEN).value(0))
                    .andExpect(jsonPath(JSON_NUMBER).value(PAGE_0))
                    .andExpect(jsonPath(JSON_SIZE).value(SIZE_9))
                    .andExpect(jsonPath(JSON_TOTAL_ELEMENTS).value(0));

            ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
            then(likeService).should().getLikedProducts(eq(USER_ID), pageableCap.capture());
            Pageable sent = pageableCap.getValue();
            assertThat(sent.getSort()).isEqualTo(SORT_BY_UPDATED_DESC);
            assertThat(sent.getPageNumber()).isEqualTo(PAGE_0);
            assertThat(sent.getPageSize()).isEqualTo(SIZE_9);
        }

        @Test @DisplayName("커스텀 page/size + sort (데이터 반환)")
        void getMyLikes_customSort_hasData() throws Exception {
            Product p = product();
            ProductSummaryResponse dto = dtoWithAvg(p, STAR_4_0, STAR_2_0);
            Pageable expected = PageRequest.of(PAGE_2, SIZE_5, SORT_BY_PRICE_DESC);
            Page<ProductSummaryResponse> stub = new PageImpl<>(List.of(dto), expected, 11);

            given(likeService.getLikedProducts(eq(USER_ID), any(Pageable.class))).willReturn(stub);

            mockMvc.perform(get(EP_GET_MY_LIKES)
                            .param("page", String.valueOf(PAGE_2))
                            .param("size", String.valueOf(SIZE_5))
                            .param("sort", SORT_PRICE + ",desc")
                            .with(authentication(authUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_CONTENT_0_PRODUCT_ID).value(PRODUCT_ID))
                    .andExpect(jsonPath(JSON_CONTENT_0_PRODUCT_NAME).value(PRODUCT_NAME_1))
                    .andExpect(jsonPath(JSON_CONTENT_0_AVG_STAR).value(AVG_3_0))
                    .andExpect(jsonPath(JSON_NUMBER).value(PAGE_2))
                    .andExpect(jsonPath(JSON_SIZE).value(SIZE_5))
                    .andExpect(jsonPath(JSON_TOTAL_ELEMENTS).value(11));

            ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
            then(likeService).should().getLikedProducts(eq(USER_ID), pageableCap.capture());
            Pageable sent = pageableCap.getValue();
            assertThat(sent.getSort()).isEqualTo(SORT_BY_PRICE_DESC);
            assertThat(sent.getPageNumber()).isEqualTo(PAGE_2);
            assertThat(sent.getPageSize()).isEqualTo(SIZE_5);
        }

        @Test @DisplayName("초과 페이지 요청 → 빈 페이지 반환")
        void getMyLikes_overflowPage_emptyButMeta() throws Exception {
            Pageable overflow = PageRequest.of(PAGE_5, SIZE_9, DEFAULT_SORT_UPDATED_DESC);
            Page<ProductSummaryResponse> stub = new PageImpl<>(List.of(), overflow, 3);
            given(likeService.getLikedProducts(eq(USER_ID), any(Pageable.class))).willReturn(stub);

            mockMvc.perform(get(EP_GET_MY_LIKES)
                            .param("page", String.valueOf(PAGE_5))
                            .with(authentication(authUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_CONTENT_LEN).value(0))
                    .andExpect(jsonPath(JSON_NUMBER).value(PAGE_5))
                    .andExpect(jsonPath(JSON_SIZE).value(SIZE_9))
                    .andExpect(jsonPath(JSON_TOTAL_ELEMENTS).value(3));
        }

        @Test @DisplayName("잘못된 정렬 → 400")
        void getMyLikes_invalidSort() throws Exception {
            mockMvc.perform(get(EP_GET_MY_LIKES)
                            .param("sort", SORT_UPDATED_AT + ",down")
                            .with(authentication(authUser())))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("401 인증 없음")
        void getMyLikes_unauth() throws Exception {
            mockMvc.perform(get(EP_GET_MY_LIKES))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("404 USER_NOT_FOUND (인증 OK, DB 미존재)")
        void getMyLikes_userNotFound() throws Exception {
            willThrow(new MemberException(com.talktrip.talktrip.global.exception.ErrorCode.USER_NOT_FOUND))
                    .given(likeService).getLikedProducts(eq(USER_ID), any(Pageable.class));

            mockMvc.perform(get(EP_GET_MY_LIKES).with(authentication(authUser())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_USER_NOT_FOUND))
                    .andExpect(jsonPath(JSON_MESSAGE).value(MSG_USER_NOT_FOUND));
        }
    }
}
