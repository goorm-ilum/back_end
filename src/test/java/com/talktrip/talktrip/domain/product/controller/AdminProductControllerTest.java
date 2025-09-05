package com.talktrip.talktrip.domain.product.controller;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductEditResponse;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductSummaryResponse;
import com.talktrip.talktrip.domain.product.service.AdminProductService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminProductController.class)
@Import(AdminProductControllerTest.TestSecurityConfig.class)
class AdminProductControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filter(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/admin/**").authenticated()
                            .anyRequest().permitAll()
                    )
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminProductService adminProductService;

    private CustomMemberDetails sellerDetails;

    @BeforeEach
    void setUp() {
        Member seller = Member.builder()
                .Id(1L)
                .accountEmail("seller@test.com")
                .phoneNum("010-9999-0000")
                .name("판매자")
                .nickname("셀러")
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
        sellerDetails = new CustomMemberDetails(seller);
    }

    @Test
    @DisplayName("판매자 상품 등록 - 멀티파트 성공 (201)")
    void createProduct_Success() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request.json", MediaType.APPLICATION_JSON_VALUE,
                """
                {
                  "productName": "제주도 여행",
                  "description": "아름다운 제주를 둘러보는 2박 3일 패키지입니다.",
                  "countryName": "대한민국",
                  "options": [
                    {
                      "startDate": "2025-09-01",
                      "optionName": "기본 2박3일",
                      "stock": 20,
                      "price": 100000,
                      "discountPrice": 80000
                    }
                  ],
                  "hashtags": ["제주", "휴양"]
                }
                """.getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile thumbnail = new MockMultipartFile(
                "thumbnailImage", "thumb.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile detail1 = new MockMultipartFile(
                "detailImages", "d1.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-image-1".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile detail2 = new MockMultipartFile(
                "detailImages", "d2.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-image-2".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/products")
                        .file(requestPart)
                        .file(thumbnail)
                        .file(detail1)
                        .file(detail2)
                        .with(user(sellerDetails))
                        .characterEncoding("UTF-8"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("판매자 상품 목록 조회 - 성공 (200)")
    void getMyProducts_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));
        AdminProductSummaryResponse summary = AdminProductSummaryResponse.builder()
                .id(1L)
                .productName("제주도 여행")
                .build();
        Page<AdminProductSummaryResponse> page =
                new PageImpl<>(List.of(summary), pageable, 1);

        when(adminProductService.getMyProducts(sellerDetails.getId(), null, "ACTIVE", pageable))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/products")
                        .with(user(sellerDetails))
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "updatedAt,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].productName").value("제주도 여행"));
    }

    @Test
    @DisplayName("판매자 상품 상세 조회 - 성공 (200)")
    void getProductDetail_Success() throws Exception {
        Long productId = 1L;

        AdminProductEditResponse edit = AdminProductEditResponse.builder()
                .productName("제주도 여행")
                .build();

        when(adminProductService.getMyProductEditForm(productId, sellerDetails.getId()))
                .thenReturn(edit);

        mockMvc.perform(get("/api/admin/products/{productId}", productId)
                        .with(user(sellerDetails))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("제주도 여행"));
    }

    @Test
    @DisplayName("판매자 상품 수정(멀티파트 PUT) - 성공 (200)")
    void updateProduct_Success() throws Exception {
        Long productId = 1L;

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request.json", MediaType.APPLICATION_JSON_VALUE,
                """
                {
                  "productName": "제주도 여행(수정)",
                  "description": "더 아름다운 제주 여행 코스 설명입니다.",
                  "countryName": "대한민국",
                  "options": [
                    {
                      "startDate": "2025-09-10",
                      "optionName": "프리미엄 3박4일",
                      "stock": 15,
                      "price": 150000,
                      "discountPrice": 120000
                    }
                  ],
                  "hashtags": ["제주", "업데이트"],
                  "existingThumbnailHash": "abc123thumbhash",
                  "existingDetailImageIds": [10, 11]
                }
                """.getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile newThumb = new MockMultipartFile(
                "thumbnailImage", "new-thumb.jpg", MediaType.IMAGE_JPEG_VALUE,
                "thumb".getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile newDetail = new MockMultipartFile(
                "detailImages", "new-d1.jpg", MediaType.IMAGE_JPEG_VALUE,
                "detail".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/products/{productId}", productId)
                        .file(requestPart)
                        .file(newThumb)
                        .file(newDetail)
                        .param("detailImageOrder", "new-d1.jpg", "old-2.jpg")
                        .with(user(sellerDetails))
                        .with(req -> { req.setMethod("PUT"); return req; })
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("판매자 상품 삭제 - 성공 (204)")
    void deleteProduct_Success() throws Exception {
        Long productId = 1L;

        mockMvc.perform(delete("/api/admin/products/{productId}", productId)
                        .with(user(sellerDetails)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("소프트 삭제 상품 복구 - 성공 (204)")
    void restoreProduct_Success() throws Exception {
        Long productId = 1L;

        mockMvc.perform(post("/api/admin/products/{productId}/restore", productId)
                        .with(user(sellerDetails)))
                .andExpect(status().isNoContent());
    }

    private static MockMultipartFile createJsonPart(String json) {
        return new MockMultipartFile(
                "request", "request.json", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8)
        );
    }
}
