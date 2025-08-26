package com.talktrip.talktrip.domain.like.controller;

import com.talktrip.talktrip.domain.like.service.LikeService;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
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

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LikeController.class)
@Import(LikeControllerTest.TestSecurityConfig.class)
class LikeControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filter(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/me/**", "/api/products/*/like").authenticated()
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
    private LikeService likeService;

    private CustomMemberDetails memberDetails;

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
        
        memberDetails = new CustomMemberDetails(member);
    }

    @Test
    @DisplayName("좋아요를 토글할 수 있다")
    void toggleLike() throws Exception {
        // given
        Long productId = 1L;
        Long memberId = memberDetails.getId();

        doNothing().when(likeService).toggleLike(eq(productId), eq(memberId));

        // when & then
        mockMvc.perform(post("/api/products/{productId}/like", productId)
                        .with(user(memberDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("좋아요 상품 목록을 조회할 수 있다")
    void getMyLikes() throws Exception {
        // given
        Long memberId = memberDetails.getId();
        Pageable pageable = PageRequest.of(0, 9, Sort.by(Sort.Direction.DESC, "updatedAt"));
        
        ProductSummaryResponse response = ProductSummaryResponse.builder()
                .productId(1L)
                .productName("제주도 여행")
                .productDescription("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .minPrice(10000)
                .minDiscountPrice(8000)
                .averageReviewStar(4.5)
                .isLiked(true)
                .build();
        
        Page<ProductSummaryResponse> page = new PageImpl<>(List.of(response), pageable, 1);

        doReturn(page).when(likeService).getLikedProducts(eq(memberId), any(Pageable.class));

        // when & then
        mockMvc.perform(get("/api/me/likes")
                        .param("page", "0")
                        .param("size", "9")
                        .param("sort", "updatedAt,desc")
                        .with(user(memberDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].productId").value(1L))
                .andExpect(jsonPath("$.content[0].productName").value("제주도 여행"))
                .andExpect(jsonPath("$.content[0].isLiked").value(true));
    }

    @Test
    @DisplayName("좋아요 상품 목록 조회 시 페이지네이션을 적용한다")
    void getMyLikes_WithPagination() throws Exception {
        // given
        Long memberId = memberDetails.getId();
        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "updatedAt"));
        
        ProductSummaryResponse response = ProductSummaryResponse.builder()
                .productId(2L)
                .productName("부산 여행")
                .productDescription("아름다운 부산 여행")
                .thumbnailImageUrl("https://example.com/busan.jpg")
                .minPrice(15000)
                .minDiscountPrice(12000)
                .averageReviewStar(4.0)
                .isLiked(true)
                .build();
        
        Page<ProductSummaryResponse> page = new PageImpl<>(List.of(response), pageable, 1);

        doReturn(page).when(likeService).getLikedProducts(eq(memberId), any(Pageable.class));

        // when & then
        mockMvc.perform(get("/api/me/likes")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "updatedAt,desc")
                        .with(user(memberDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].productId").value(2L))
                .andExpect(jsonPath("$.content[0].productName").value("부산 여행"));
    }

    @Test
    @DisplayName("좋아요 상품이 없을 때 빈 페이지를 반환한다")
    void getMyLikes_EmptyResult() throws Exception {
        // given
        Long memberId = memberDetails.getId();
        Pageable pageable = PageRequest.of(0, 9, Sort.by(Sort.Direction.DESC, "updatedAt"));
        
        Page<ProductSummaryResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        doReturn(emptyPage).when(likeService).getLikedProducts(eq(memberId), any(Pageable.class));

        // when & then
        mockMvc.perform(get("/api/me/likes")
                        .param("page", "0")
                        .param("size", "9")
                        .with(user(memberDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("좋아요 토글 시 null productId로 validation 에러가 발생한다")
    void toggleLike_NullProductId_ThrowsValidationException() throws Exception {
        // when & then
        mockMvc.perform(post("/api/products/{productId}/like", (Object) null)
                        .with(user(memberDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비로그인 사용자로 좋아요 토글 시 401 오류를 반환한다")
    void toggleLike_Unauthorized() throws Exception {
        // given
        Long productId = 1L;

        // when & then
        mockMvc.perform(post("/api/products/{productId}/like", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비로그인 사용자로 좋아요 목록 조회 시 401 오류를 반환한다")
    void getMyLikes_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/me/likes")
                        .param("page", "0")
                        .param("size", "9")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("좋아요 목록 조회 시 정렬 파라미터가 올바르게 적용된다")
    void getMyLikes_WithSorting() throws Exception {
        // given
        Long memberId = memberDetails.getId();
        Pageable pageable = PageRequest.of(0, 9, Sort.by(Sort.Direction.ASC, "productName"));
        
        ProductSummaryResponse response = ProductSummaryResponse.builder()
                .productId(1L)
                .productName("제주도 여행")
                .productDescription("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .minPrice(10000)
                .minDiscountPrice(8000)
                .averageReviewStar(4.5)
                .isLiked(true)
                .build();
        
        Page<ProductSummaryResponse> page = new PageImpl<>(List.of(response), pageable, 1);

        doReturn(page).when(likeService).getLikedProducts(eq(memberId), any(Pageable.class));

        // when & then
        mockMvc.perform(get("/api/me/likes")
                        .param("page", "0")
                        .param("size", "9")
                        .param("sort", "productName,asc")
                        .with(user(memberDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
