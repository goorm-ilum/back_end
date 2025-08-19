package com.talktrip.talktrip.domain.like.controller;

import com.talktrip.talktrip.domain.like.service.LikeService;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
        @Bean
        LikeService likeService() {
            return mock(LikeService.class);
        }
    }

    @AfterEach
    void tearDown() {
        reset(likeService);
    }

    private Member member(long id, String email) {
        return Member.builder()
                .Id(id)
                .accountEmail(email)
                .memberRole(MemberRole.A)
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

    private UsernamePasswordAuthenticationToken auth(long memberId) {
        CustomMemberDetails md = mock(CustomMemberDetails.class);
        org.mockito.Mockito.when(md.getId()).thenReturn(memberId);
        return new UsernamePasswordAuthenticationToken(
                md,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Nested
    @DisplayName("POST /api/products/{productId}/like")
    class ToggleLike {

        @Test
        @DisplayName("정상 요청: 200 OK & 서비스에 productId/principal 전달")
        void ok() throws Exception {
            long userId = 1L;
            long productId = 3L;

            mockMvc.perform(post("/api/products/{productId}/like", productId)
                            .with(authentication(auth(userId)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            ArgumentCaptor<Long> productIdCap = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<CustomMemberDetails> principalCap = ArgumentCaptor.forClass(CustomMemberDetails.class);

            then(likeService).should().toggleLike(productIdCap.capture(), principalCap.capture());
            assertThat(productIdCap.getValue()).isEqualTo(productId);
            assertThat(principalCap.getValue().getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("인증 없음: 401 Unauthorized")
        void toggleLike_unauthenticated() throws Exception {
            long productId = 3L;

            mockMvc.perform(post("/api/products/{productId}/like", productId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("사용자 없음: 404")
        void toggleLike_userNotFound() throws Exception {
            long userId = 1L;
            long productId = 3L;

            willThrow(new com.talktrip.talktrip.global.exception.MemberException(
                    com.talktrip.talktrip.global.exception.ErrorCode.USER_NOT_FOUND
            )).given(likeService).toggleLike(anyLong(), any(CustomMemberDetails.class));

            mockMvc.perform(post("/api/products/{productId}/like", productId)
                            .with(authentication(auth(userId)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("상품 없음: 404")
        void toggleLike_productNotFound() throws Exception {
            long userId = 1L;
            long productId = 3L;

            willThrow(new com.talktrip.talktrip.global.exception.ProductException(
                    com.talktrip.talktrip.global.exception.ErrorCode.PRODUCT_NOT_FOUND
            )).given(likeService).toggleLike(anyLong(), any(CustomMemberDetails.class));

            mockMvc.perform(post("/api/products/{productId}/like", productId)
                            .with(authentication(auth(userId)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다."));
        }
    }

    @Nested
    @DisplayName("GET /api/me/likes")
    class GetMyLikes {

        @Test
        @DisplayName("기본 파라미터 page=0,size=9,sort=updatedAt,desc")
        void defaultParams() throws Exception {
            long userId = 1L;
            long sellerId = 2L;
            long productId = 3L;

            Member seller = member(sellerId, "seller@gmail.com");
            Product p = product(productId, seller);

            Review r1 = Review.builder().reviewStar(5f).product(p).build();
            Review r2 = Review.builder().reviewStar(4f).product(p).build();
            p.getReviews().addAll(List.of(r1, r2));
            float avg = (r1.getReviewStar() + r2.getReviewStar()) / 2f;

            ProductSummaryResponse dto = ProductSummaryResponse.from(p, avg, true);

            Sort expectedSort = Sort.by(Sort.Order.desc("updatedAt"));
            Pageable expected = PageRequest.of(0, 9, expectedSort);
            Page<ProductSummaryResponse> stub = new PageImpl<>(List.of(dto), expected, 1);

            given(likeService.getLikedProducts(any(CustomMemberDetails.class), any(Pageable.class)))
                    .willReturn(stub);

            mockMvc.perform(get("/api/me/likes").with(authentication(auth(userId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].productId").value(productId))
                    .andExpect(jsonPath("$.content[0].productName").value("P1"))
                    .andExpect(jsonPath("$.content[0].averageReviewStar").value(4.5))
                    .andExpect(jsonPath("$.content[0].isLiked").value(true))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.size").value(9))
                    .andExpect(jsonPath("$.totalElements").value(1));

            ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
            then(likeService).should().getLikedProducts(any(CustomMemberDetails.class), pageableCap.capture());

            Pageable sent = pageableCap.getValue();
            assertThat(sent.getSort()).isEqualTo(expectedSort);
            assertThat(sent.getPageNumber()).isEqualTo(0);
            assertThat(sent.getPageSize()).isEqualTo(9);
        }

        @Test
        @DisplayName("커스텀 page/size + sort")
        void customMultiSort() throws Exception {
            long userId = 1L;
            long sellerId = 2L;
            long productId = 3L;

            Member seller = member(sellerId, "seller2@gmail.com");
            Product p = product(productId, seller);

            Review r1 = Review.builder().reviewStar(4f).product(p).build();
            Review r2 = Review.builder().reviewStar(2f).product(p).build();
            p.getReviews().addAll(List.of(r1, r2));
            float avg = (r1.getReviewStar() + r2.getReviewStar()) / 2f;

            ProductSummaryResponse dto = ProductSummaryResponse.from(p, avg, true);

            Sort expectedSort = Sort.by(Sort.Order.desc("price"));
            Pageable expected = PageRequest.of(2, 5, expectedSort);
            Page<ProductSummaryResponse> stub = new PageImpl<>(List.of(dto), expected, 11);

            given(likeService.getLikedProducts(any(CustomMemberDetails.class), any(Pageable.class)))
                    .willReturn(stub);

            mockMvc.perform(get("/api/me/likes")
                            .param("page", "2")
                            .param("size", "5")
                            .param("sort", "price,desc")
                            .with(authentication(auth(userId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].productId").value(productId))
                    .andExpect(jsonPath("$.content[0].productName").value("P1"))
                    .andExpect(jsonPath("$.content[0].averageReviewStar").value(3.0))
                    .andExpect(jsonPath("$.number").value(2))
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11));

            ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
            then(likeService).should().getLikedProducts(any(CustomMemberDetails.class), pageableCap.capture());

            Pageable sent = pageableCap.getValue();
            assertThat(sent.getSort()).isEqualTo(expectedSort);
            assertThat(sent.getPageNumber()).isEqualTo(2);
            assertThat(sent.getPageSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("빈 페이지 응답")
        void emptyPage() throws Exception {
            long userId = 1L;

            Pageable expected = PageRequest.of(0, 9, Sort.by(Sort.Order.desc("updatedAt")));
            Page<ProductSummaryResponse> stub = new PageImpl<>(List.of(), expected, 0);

            given(likeService.getLikedProducts(any(CustomMemberDetails.class), any(Pageable.class)))
                    .willReturn(stub);

            mockMvc.perform(get("/api/me/likes").with(authentication(auth(userId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("인증 없음: 401 Unauthorized")
        void getMyLikes_unauthenticated() throws Exception {
            mockMvc.perform(get("/api/me/likes"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("사용자 없음: 404 Not Found")
        void getMyLikes_userNotFound() throws Exception {
            long userId = 1L;

            willThrow(new com.talktrip.talktrip.global.exception.MemberException(
                    com.talktrip.talktrip.global.exception.ErrorCode.USER_NOT_FOUND
            )).given(likeService).getLikedProducts(any(CustomMemberDetails.class), any(Pageable.class));

            mockMvc.perform(get("/api/me/likes")
                            .with(authentication(auth(userId))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("상품 없음: 404 Not Found")
        void getMyLikes_productNotFound() throws Exception {
            long userId = 1L;

            willThrow(new com.talktrip.talktrip.global.exception.ProductException(
                    com.talktrip.talktrip.global.exception.ErrorCode.PRODUCT_NOT_FOUND
            )).given(likeService).getLikedProducts(any(CustomMemberDetails.class), any(Pageable.class));

            mockMvc.perform(get("/api/me/likes")
                            .with(authentication(auth(userId))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다."));
        }
    }
}
