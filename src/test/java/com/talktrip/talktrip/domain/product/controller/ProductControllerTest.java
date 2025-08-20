package com.talktrip.talktrip.domain.product.controller;

import com.talktrip.talktrip.domain.product.dto.response.ProductDetailResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductOptionResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.service.ProductService;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ProductException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = ProductController.class)
@Import({ProductControllerTest.TestConfig.class, ProductControllerTest.SecurityPermitAllTestConfig.class})
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ProductService productService;

    @TestConfiguration
    static class TestConfig {
        @Bean ProductService productService() { return mock(ProductService.class); }
    }

    @TestConfiguration
    static class SecurityPermitAllTestConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    private UsernamePasswordAuthenticationToken auth() {
        CustomMemberDetails md = mock(CustomMemberDetails.class);
        given(md.getId()).willReturn(USER_ID);
        return new UsernamePasswordAuthenticationToken(md, null,
                List.of(new SimpleGrantedAuthority(ROLE_USER)));
    }

    private ProductDetailResponse detailDto(boolean liked) {
        return new ProductDetailResponse(
                PRODUCT_ID, PRODUCT_NAME_1, DESC,
                PRICE_12000, DISC_9000,
                LocalDateTime.parse(TIME),
                THUMBNAIL_URL, COUNTRY_KOREA,
                List.of(HASHTAG_SEA, HASHTAG_FOOD),
                List.of(IMAGE_URL_1, IMAGE_URL_2),
                List.of(new ProductOptionResponse(OPTION_ID_1, OPTION_NAME, LocalDate.now(), STOCK_5, PRICE_12000, DISC_9000)),
                STAR_4_5,
                List.of(),
                liked,
                SELLER_NAME, SELLER_EMAIL, PHONE_NUMBER
        );
    }

    @AfterEach
    void resetMocks() { reset(productService); }

    @Nested
    @DisplayName("GET " + EP_GET_PRODUCT_DETAIL + " (상품 상세)")
    class GetDetail {

        @Test @DisplayName("200 OK (비로그인 isLiked=false)")
        void ok_guest() throws Exception {
            given(productService.getProductDetail(eq(PRODUCT_ID), isNull(), any(Pageable.class)))
                    .willReturn(detailDto(false));

            mockMvc.perform(get(EP_GET_PRODUCT_DETAIL, PRODUCT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PRODUCT_ID).value(PRODUCT_ID))
                    .andExpect(jsonPath(JSON_IS_LIKED).value(false));
        }

        @Test @DisplayName("200 OK (로그인 isLiked=true)")
        void ok_authed() throws Exception {
            given(productService.getProductDetail(eq(PRODUCT_ID), eq(USER_ID), any(Pageable.class)))
                    .willReturn(detailDto(true));

            mockMvc.perform(get(EP_GET_PRODUCT_DETAIL, PRODUCT_ID)
                            .with(authentication(auth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PRODUCT_ID).value(PRODUCT_ID))
                    .andExpect(jsonPath(JSON_IS_LIKED).value(true));
        }

        @Test @DisplayName("404 PRODUCT_NOT_FOUND")
        void notFound() throws Exception {
            willThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND))
                    .given(productService).getProductDetail(eq(PRODUCT_ID), any(), any(Pageable.class));

            mockMvc.perform(get(EP_GET_PRODUCT_DETAIL, PRODUCT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_PRODUCT_NOT_FOUND));
        }

        @Test @DisplayName("404 PRODUCT_NOT_FOUND (재고 합=0)")
        void futureStockZero() throws Exception {
            willThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND))
                    .given(productService).getProductDetail(eq(PRODUCT_ID), any(), any(Pageable.class));

            mockMvc.perform(get(EP_GET_PRODUCT_DETAIL, PRODUCT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_PRODUCT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("GET " + EP_SEARCH_PRODUCTS + " (상품 검색)")
    class Search {

        @Test @DisplayName("기본 page=0,size=10,sort=updatedAt,desc (빈 페이지)")
        void defaultEmpty() throws Exception {
            Page<ProductSummaryResponse> stub = new PageImpl<>(List.of(), PAGE_0_SIZE_10, 0);
            given(productService.searchProducts(isNull(), anyString(), isNull(), any(Pageable.class)))
                    .willReturn(stub);

            mockMvc.perform(get(EP_SEARCH_PRODUCTS)
                            .param("countryName", COUNTRY_KOREA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_CONTENT_LEN).value(0))
                    .andExpect(jsonPath(JSON_NUMBER).value(PAGE_0))
                    .andExpect(jsonPath(JSON_SIZE).value(SIZE_10));
        }

        @Test @DisplayName("커스텀 page/size + sort (데이터 반환)")
        void custom_hasData() throws Exception {
            ProductSummaryResponse dto = new ProductSummaryResponse(
                    PRODUCT_ID, PRODUCT_NAME_1, DESC, THUMBNAIL_URL,
                    PRICE_12000, DISC_9000, STAR_4_0, false
            );

            Pageable expected = PageRequest.of(PAGE_2, SIZE_5, SORT_BY_PRODUCT_NAME_DESC);
            Page<ProductSummaryResponse> stub = new PageImpl<>(List.of(dto), expected, 11);

            given(productService.searchProducts(eq(PRODUCT_NAME_1), eq(COUNTRY_KOREA), isNull(), any(Pageable.class)))
                    .willReturn(stub);

            mockMvc.perform(get(EP_SEARCH_PRODUCTS)
                            .param("keyword", PRODUCT_NAME_1)
                            .param("countryName", COUNTRY_KOREA)
                            .param("page", String.valueOf(PAGE_2))
                            .param("size", String.valueOf(SIZE_5))
                            .param("sort", SORT_PRODUCT_NAME + ",desc")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_CONTENT_0_PRODUCT_ID).value(PRODUCT_ID))
                    .andExpect(jsonPath(JSON_CONTENT_0_PRODUCT_NAME).value(PRODUCT_NAME_1))
                    .andExpect(jsonPath(JSON_NUMBER).value(PAGE_2))
                    .andExpect(jsonPath(JSON_SIZE).value(SIZE_5))
                    .andExpect(jsonPath(JSON_TOTAL_ELEMENTS).value(11));
        }

        @Test @DisplayName("로그인 시 memberId 전달")
        void passMemberIdWhenAuthed() throws Exception {
            Page<ProductSummaryResponse> stub = new PageImpl<>(
                    List.of(new ProductSummaryResponse(PRODUCT_ID, PRODUCT_NAME_1, DESC, THUMBNAIL_URL,
                            PRICE_12000, DISC_9000, STAR_4_0, true)),
                    PAGE_0_SIZE_10, 1);
            given(productService.searchProducts(eq(PRODUCT_NAME_1), eq(COUNTRY_KOREA), eq(USER_ID), any(Pageable.class)))
                    .willReturn(stub);

            mockMvc.perform(get(EP_SEARCH_PRODUCTS)
                            .param("keyword", PRODUCT_NAME_1)
                            .param("countryName", COUNTRY_KOREA)
                            .with(authentication(auth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_CONTENT_0_PRODUCT_NAME).value(PRODUCT_NAME_1));
        }

        @Test @DisplayName("keyword 미제공 → null로 서비스에 전달")
        void keyword_absent_pass_null() throws Exception {
            Page<ProductSummaryResponse> stub = new PageImpl<>(List.of(), PAGE_0_SIZE_10, 0);
            given(productService.searchProducts(isNull(), eq(COUNTRY_KOREA), isNull(), any(Pageable.class)))
                    .willReturn(stub);

            mockMvc.perform(get(EP_SEARCH_PRODUCTS)
                            .param("countryName", COUNTRY_KOREA))
                    .andExpect(status().isOk());

            ArgumentCaptor<String> kwCap = ArgumentCaptor.forClass(String.class);
            then(productService).should()
                    .searchProducts(kwCap.capture(), eq(COUNTRY_KOREA), isNull(), any(Pageable.class));
            assertThat(kwCap.getValue()).isNull();
        }

        @Test @DisplayName("keyword 공백 문자열 → 그대로 서비스에 전달(트림/토크나이즈는 서비스 책임)")
        void keyword_blank_string_passed_as_is() throws Exception {
            Page<ProductSummaryResponse> stub = new PageImpl<>(List.of(), PAGE_0_SIZE_10, 0);
            given(productService.searchProducts(eq(BLANK), eq(COUNTRY_KOREA), isNull(), any(Pageable.class)))
                    .willReturn(stub);

            mockMvc.perform(get(EP_SEARCH_PRODUCTS)
                            .param("keyword", BLANK)
                            .param("countryName", COUNTRY_KOREA))
                    .andExpect(status().isOk());

            ArgumentCaptor<String> kwCap = ArgumentCaptor.forClass(String.class);
            then(productService).should()
                    .searchProducts(kwCap.capture(), eq(COUNTRY_KOREA), isNull(), any(Pageable.class));
            assertThat(kwCap.getValue()).isEqualTo(BLANK);
        }

        @Test @DisplayName("keyword 다중 단어 문자열 → 그대로 서비스에 전달(분리는 서비스에서 수행)")
        void keyword_multi_word_passed_as_is() throws Exception {
            String multi = PRODUCT_NAME_1 + " " + HASHTAG_SEA;

            Page<ProductSummaryResponse> stub = new PageImpl<>(List.of(), PAGE_0_SIZE_10, 0);
            given(productService.searchProducts(eq(multi), eq(COUNTRY_KOREA), isNull(), any(Pageable.class)))
                    .willReturn(stub);

            mockMvc.perform(get(EP_SEARCH_PRODUCTS)
                            .param("keyword", multi)
                            .param("countryName", COUNTRY_KOREA))
                    .andExpect(status().isOk());

            ArgumentCaptor<String> kwCap = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
            then(productService).should()
                    .searchProducts(kwCap.capture(), eq(COUNTRY_KOREA), isNull(), pageableCap.capture());

            assertThat(kwCap.getValue()).isEqualTo(multi);
            Pageable sent = pageableCap.getValue();
            assertThat(sent.getPageNumber()).isEqualTo(PAGE_0);
            assertThat(sent.getPageSize()).isEqualTo(SIZE_10);
            assertThat(sent.getSort()).isEqualTo(DEFAULT_SORT_UPDATED_DESC);
        }
    }
}
