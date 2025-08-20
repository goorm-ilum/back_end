package com.talktrip.talktrip.domain.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductCreateRequest;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductUpdateRequest;
import com.talktrip.talktrip.domain.product.dto.request.ProductOptionRequest;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductEditResponse;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductSummaryResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductOptionResponse;
import com.talktrip.talktrip.domain.product.service.AdminProductService;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = AdminProductController.class)
@Import(AdminProductControllerTest.TestConfig.class)
class AdminProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AdminProductService adminProductService;
    @Autowired ObjectMapper om;

    @TestConfiguration
    static class TestConfig {
        @Bean AdminProductService adminProductService() { return mock(AdminProductService.class); }
    }

    private UsernamePasswordAuthenticationToken auth() {
        CustomMemberDetails md = mock(CustomMemberDetails.class);
        given(md.getId()).willReturn(SELLER_ID);
        return new UsernamePasswordAuthenticationToken(md, null,
                List.of(new SimpleGrantedAuthority(ROLE_USER)));
    }

    private AdminProductEditResponse editDto() {
        return AdminProductEditResponse.builder()
                .productName(PRODUCT_NAME_1)
                .description(DESC)
                .continent(CONTINENT_ASIA)
                .country(COUNTRY_KOREA)
                .thumbnailImageUrl(THUMBNAIL_URL)
                .thumbnailImageHash(THUMBNAIL_HASH)
                .options(List.of(new ProductOptionResponse(OPTION_ID_1, OPTION_NAME, LocalDate.now(), STOCK_3, PRICE_10000, DISC_9000)))
                .images(List.of(new AdminProductEditResponse.ImageInfo(IMAGE_ID_10, IMAGE_URL_3)))
                .hashtags(List.of(HASHTAG_SEA))
                .build();
    }

    @Nested
    @DisplayName("POST " + EP_ADMIN_CREATE_PRODUCT + " (상품 생성)")
    class Create {

        @Test @DisplayName("201 Created + 서비스 전달값 검증")
        void created_multipart_and_args() throws Exception {
            AdminProductCreateRequest req = new AdminProductCreateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA,
                    List.of(new ProductOptionRequest(LocalDate.now(), OPTION_NAME, STOCK_5, PRICE_10000, DISC_9000)),
                    List.of(HASHTAG_SEA, HASHTAG_FOOD)
            );

            MockMultipartFile requestPart = new MockMultipartFile(
                    "request", "request.json", MediaType.APPLICATION_JSON_VALUE,
                    om.writeValueAsBytes(req)
            );
            MockMultipartFile thumbnail = new MockMultipartFile("thumbnailImage", "t.png", "image/png", "bin".getBytes(StandardCharsets.UTF_8));
            MockMultipartFile detail1 = new MockMultipartFile("detailImages", "d1.png", "image/png", "bin".getBytes(StandardCharsets.UTF_8));

            willDoNothing().given(adminProductService)
                    .createProduct(any(AdminProductCreateRequest.class), eq(SELLER_ID), any(), anyList());

            mockMvc.perform(multipart(EP_ADMIN_CREATE_PRODUCT)
                            .file(requestPart)
                            .file(thumbnail)
                            .file(detail1)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isCreated());

            ArgumentCaptor<AdminProductCreateRequest> cap = ArgumentCaptor.forClass(AdminProductCreateRequest.class);
            then(adminProductService).should()
                    .createProduct(cap.capture(), eq(SELLER_ID), any(), anyList());
            assertThat(cap.getValue().productName()).isEqualTo(PRODUCT_NAME_1);
        }

        @Test @DisplayName("401 인증 없음")
        void unauth() throws Exception {
            mockMvc.perform(multipart(EP_ADMIN_CREATE_PRODUCT).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("404 COUNTRY_NOT_FOUND")
        void countryNotFound() throws Exception {
            AdminProductCreateRequest req = new AdminProductCreateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA, List.of(), List.of()
            );
            MockMultipartFile requestPart = new MockMultipartFile(
                    "request", "request.json", MediaType.APPLICATION_JSON_VALUE, om.writeValueAsBytes(req)
            );

            willThrow(new ProductException(ErrorCode.COUNTRY_NOT_FOUND))
                    .given(adminProductService).createProduct(any(), eq(SELLER_ID), isNull(), isNull());

            mockMvc.perform(multipart(EP_ADMIN_CREATE_PRODUCT)
                            .file(requestPart)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_COUNTRY_NOT_FOUND));
        }

        @Test @DisplayName("404 ADMIN_NOT_FOUND")
        void adminNotFound() throws Exception {
            AdminProductCreateRequest req = new AdminProductCreateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA, List.of(), List.of()
            );
            MockMultipartFile requestPart = new MockMultipartFile(
                    "request", "request.json", MediaType.APPLICATION_JSON_VALUE, om.writeValueAsBytes(req)
            );

            willThrow(new MemberException(ErrorCode.ADMIN_NOT_FOUND))
                    .given(adminProductService).createProduct(any(), eq(SELLER_ID), isNull(), isNull());

            mockMvc.perform(multipart(EP_ADMIN_CREATE_PRODUCT)
                            .file(requestPart)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ADMIN_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("PUT " + EP_ADMIN_UPDATE_PRODUCT + " (상품 수정)")
    class Update {

        @Test @DisplayName("200 OK")
        void ok() throws Exception {
            AdminProductUpdateRequest req = new AdminProductUpdateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA,
                    List.of(new ProductOptionRequest(LocalDate.now(), OPTION_NAME, STOCK_3, PRICE_10000, DISC_9000)),
                    List.of(HASHTAG_SEA), THUMBNAIL_HASH, List.of(1L,2L)
            );

            MockMultipartFile requestPart = new MockMultipartFile("request", "request.json",
                    MediaType.APPLICATION_JSON_VALUE, om.writeValueAsBytes(req));

            willDoNothing().given(adminProductService)
                    .updateProduct(eq(PRODUCT_ID), any(AdminProductUpdateRequest.class), eq(SELLER_ID),
                            isNull(), isNull(), isNull());

            mockMvc.perform(multipart(HttpMethod.PUT, EP_ADMIN_UPDATE_PRODUCT, PRODUCT_ID)
                            .file(requestPart)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("401 인증 없음")
        void unauth() throws Exception {
            mockMvc.perform(multipart(HttpMethod.PUT, EP_ADMIN_UPDATE_PRODUCT, PRODUCT_ID)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("404 PRODUCT_NOT_FOUND")
        void productNotFound() throws Exception {
            AdminProductUpdateRequest req = new AdminProductUpdateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA, List.of(), List.of(), THUMBNAIL_HASH, List.of()
            );
            MockMultipartFile requestPart = new MockMultipartFile("request", "request.json",
                    MediaType.APPLICATION_JSON_VALUE, om.writeValueAsBytes(req));

            willThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND))
                    .given(adminProductService)
                    .updateProduct(eq(PRODUCT_ID), any(), eq(SELLER_ID), isNull(), isNull(), isNull());

            mockMvc.perform(multipart(HttpMethod.PUT, EP_ADMIN_UPDATE_PRODUCT, PRODUCT_ID)
                            .file(requestPart)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_PRODUCT_NOT_FOUND));
        }

        @Test @DisplayName("403 ACCESS_DENIED")
        void accessDenied() throws Exception {
            AdminProductUpdateRequest req = new AdminProductUpdateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA, List.of(), List.of(), THUMBNAIL_HASH, List.of()
            );
            MockMultipartFile requestPart = new MockMultipartFile("request", "request.json",
                    MediaType.APPLICATION_JSON_VALUE, om.writeValueAsBytes(req));

            willThrow(new ProductException(ErrorCode.ACCESS_DENIED))
                    .given(adminProductService)
                    .updateProduct(eq(PRODUCT_ID), any(), eq(SELLER_ID), isNull(), isNull(), isNull());

            mockMvc.perform(multipart(HttpMethod.PUT, EP_ADMIN_UPDATE_PRODUCT, PRODUCT_ID)
                            .file(requestPart)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ACCESS_DENIED))
                    .andExpect(jsonPath(JSON_MESSAGE).value(MSG_ACCESS_DENIED));
        }

        @Test @DisplayName("404 COUNTRY_NOT_FOUND")
        void countryNotFound() throws Exception {
            AdminProductUpdateRequest req = new AdminProductUpdateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA, List.of(), List.of(), THUMBNAIL_HASH, List.of()
            );
            MockMultipartFile requestPart = new MockMultipartFile("request", "request.json",
                    MediaType.APPLICATION_JSON_VALUE, om.writeValueAsBytes(req));

            willThrow(new ProductException(ErrorCode.COUNTRY_NOT_FOUND))
                    .given(adminProductService)
                    .updateProduct(eq(PRODUCT_ID), any(), eq(SELLER_ID), isNull(), isNull(), isNull());

            mockMvc.perform(multipart(HttpMethod.PUT, EP_ADMIN_UPDATE_PRODUCT, PRODUCT_ID)
                            .file(requestPart)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_COUNTRY_NOT_FOUND));
        }

        @Test @DisplayName("500 IMAGE_UPLOAD_FAILED")
        void imageUploadFailed() throws Exception {
            AdminProductUpdateRequest req = new AdminProductUpdateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA, List.of(), List.of(), THUMBNAIL_HASH, List.of()
            );
            MockMultipartFile requestPart = new MockMultipartFile("request", "request.json",
                    MediaType.APPLICATION_JSON_VALUE, om.writeValueAsBytes(req));

            willThrow(new ProductException(ErrorCode.IMAGE_UPLOAD_FAILED))
                    .given(adminProductService)
                    .updateProduct(eq(PRODUCT_ID), any(), eq(SELLER_ID), isNull(), isNull(), isNull());

            mockMvc.perform(multipart(HttpMethod.PUT, EP_ADMIN_UPDATE_PRODUCT, PRODUCT_ID)
                            .file(requestPart)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_IMAGE_UPLOAD_FAILED));
        }
    }

    @Nested
    @DisplayName("DELETE " + EP_ADMIN_DELETE_PRODUCT + " (상품 삭제)")
    class DeleteApi {

        @Test @DisplayName("204 No Content")
        void ok() throws Exception {
            willDoNothing().given(adminProductService).deleteProduct(PRODUCT_ID, SELLER_ID);

            mockMvc.perform(delete(EP_ADMIN_DELETE_PRODUCT, PRODUCT_ID)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isNoContent());

            then(adminProductService).should().deleteProduct(PRODUCT_ID, SELLER_ID);
        }

        @Test @DisplayName("401 인증 없음")
        void unauth() throws Exception {
            mockMvc.perform(delete(EP_ADMIN_DELETE_PRODUCT, PRODUCT_ID).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("404 PRODUCT_NOT_FOUND")
        void notFound() throws Exception {
            willThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND))
                    .given(adminProductService).deleteProduct(PRODUCT_ID, SELLER_ID);

            mockMvc.perform(delete(EP_ADMIN_DELETE_PRODUCT, PRODUCT_ID)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_PRODUCT_NOT_FOUND));
        }

        @Test @DisplayName("403 ACCESS_DENIED")
        void accessDenied() throws Exception {
            willThrow(new ProductException(ErrorCode.ACCESS_DENIED))
                    .given(adminProductService).deleteProduct(PRODUCT_ID, SELLER_ID);

            mockMvc.perform(delete(EP_ADMIN_DELETE_PRODUCT, PRODUCT_ID)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ACCESS_DENIED))
                    .andExpect(jsonPath(JSON_MESSAGE).value(MSG_ACCESS_DENIED));
        }
    }

    @Nested
    @DisplayName("POST " + EP_ADMIN_RESTORE_PRODUCT + " (상품 복구)")
    class Restore {

        @Test @DisplayName("204 No Content")
        void ok() throws Exception {
            willDoNothing().given(adminProductService).restoreProduct(PRODUCT_ID, SELLER_ID);

            mockMvc.perform(post(EP_ADMIN_RESTORE_PRODUCT, PRODUCT_ID)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isNoContent());

            then(adminProductService).should().restoreProduct(PRODUCT_ID, SELLER_ID);
        }

        @Test @DisplayName("401 인증 없음")
        void unauth() throws Exception {
            mockMvc.perform(post(EP_ADMIN_RESTORE_PRODUCT, PRODUCT_ID).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("404 PRODUCT_NOT_FOUND")
        void notFound() throws Exception {
            willThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND))
                    .given(adminProductService).restoreProduct(PRODUCT_ID, SELLER_ID);

            mockMvc.perform(post(EP_ADMIN_RESTORE_PRODUCT, PRODUCT_ID)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_PRODUCT_NOT_FOUND));
        }

        @Test @DisplayName("403 ACCESS_DENIED")
        void accessDenied() throws Exception {
            willThrow(new ProductException(ErrorCode.ACCESS_DENIED))
                    .given(adminProductService).restoreProduct(PRODUCT_ID, SELLER_ID);

            mockMvc.perform(post(EP_ADMIN_RESTORE_PRODUCT, PRODUCT_ID)
                            .with(authentication(auth())).with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ACCESS_DENIED))
                    .andExpect(jsonPath(JSON_MESSAGE).value(MSG_ACCESS_DENIED));
        }
    }

    @Nested
    @DisplayName("GET " + EP_ADMIN_LIST_PRODUCTS + " (내 상품 목록)")
    class AdminList {

        @Test @DisplayName("기본 page=0,size=10,sort=updatedAt,desc (빈 페이지)")
        void defaultEmpty() throws Exception {
            Page<AdminProductSummaryResponse> stub = new PageImpl<>(List.of(), PAGE_0_SIZE_10, 0);
            given(adminProductService.getMyProducts(eq(SELLER_ID), isNull(), eq("ACTIVE"), any(Pageable.class)))
                    .willReturn(stub);

            mockMvc.perform(get(EP_ADMIN_LIST_PRODUCTS).with(authentication(auth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_CONTENT_LEN).value(0))
                    .andExpect(jsonPath(JSON_NUMBER).value(PAGE_0))
                    .andExpect(jsonPath(JSON_SIZE).value(SIZE_10));
        }

        @Test @DisplayName("키워드/상태/정렬/페이지 파라미터 전달 검증 + 데이터 반환")
        void custom_hasData_and_params() throws Exception {
            AdminProductSummaryResponse dto = AdminProductSummaryResponse.builder()
                    .id(PRODUCT_ID).productName(PRODUCT_NAME_1)
                    .thumbnailImageUrl(THUMBNAIL_URL).isDeleted(false)
                    .totalStock(STOCK_3).updatedAt(LocalDateTime.parse(TIME)).build();

            Pageable expected = PageRequest.of(PAGE_2, SIZE_5, SORT_BY_PRODUCT_NAME_DESC);
            Page<AdminProductSummaryResponse> stub = new PageImpl<>(List.of(dto), expected, 11);

            given(adminProductService.getMyProducts(eq(SELLER_ID), eq(KEYWORD_MULTI), eq(STATUS_DELETED), any(Pageable.class)))
                    .willReturn(stub);

            mockMvc.perform(get(EP_ADMIN_LIST_PRODUCTS)
                            .param("keyword", KEYWORD_MULTI)
                            .param("status", STATUS_DELETED)
                            .param("page", String.valueOf(PAGE_2))
                            .param("size", String.valueOf(SIZE_5))
                            .param("sort", SORT_PRODUCT_NAME + ",desc")
                            .with(authentication(auth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_CONTENT_0_PRODUCT_NAME).value(PRODUCT_NAME_1))
                    .andExpect(jsonPath(JSON_NUMBER).value(PAGE_2))
                    .andExpect(jsonPath(JSON_SIZE).value(SIZE_5))
                    .andExpect(jsonPath(JSON_TOTAL_ELEMENTS).value(11));

            ArgumentCaptor<String> kwCap = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> stCap = ArgumentCaptor.forClass(String.class);
            then(adminProductService).should()
                    .getMyProducts(eq(SELLER_ID), kwCap.capture(), stCap.capture(), any(Pageable.class));
            assertThat(kwCap.getValue()).isEqualTo(KEYWORD_MULTI);
            assertThat(stCap.getValue()).isEqualTo(STATUS_DELETED);
        }
    }

    @Nested
    @DisplayName("GET " + EP_ADMIN_EDIT_FORM + " (편집 폼)")
    class EditForm {
        @Test @DisplayName("200 OK")
        void ok() throws Exception {
            given(adminProductService.getMyProductEditForm(eq(PRODUCT_ID), eq(SELLER_ID))).willReturn(editDto());

            mockMvc.perform(get(EP_ADMIN_EDIT_FORM, PRODUCT_ID).with(authentication(auth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PRODUCT_NAME).value(PRODUCT_NAME_1));
        }

        @Test @DisplayName("401 인증 없음")
        void unauth() throws Exception {
            mockMvc.perform(get(EP_ADMIN_EDIT_FORM, PRODUCT_ID))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("404 PRODUCT_NOT_FOUND")
        void notFound() throws Exception {
            willThrow(new ProductException(ErrorCode.PRODUCT_NOT_FOUND))
                    .given(adminProductService).getMyProductEditForm(eq(PRODUCT_ID), eq(SELLER_ID));

            mockMvc.perform(get(EP_ADMIN_EDIT_FORM, PRODUCT_ID).with(authentication(auth())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_PRODUCT_NOT_FOUND));
        }

        @Test @DisplayName("403 ACCESS_DENIED")
        void accessDenied() throws Exception {
            willThrow(new ProductException(ErrorCode.ACCESS_DENIED))
                    .given(adminProductService).getMyProductEditForm(eq(PRODUCT_ID), eq(SELLER_ID));

            mockMvc.perform(get(EP_ADMIN_EDIT_FORM, PRODUCT_ID).with(authentication(auth())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(ERR_ACCESS_DENIED))
                    .andExpect(jsonPath(JSON_MESSAGE).value(MSG_ACCESS_DENIED));
        }
    }
}
