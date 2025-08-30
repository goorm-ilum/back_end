package com.talktrip.talktrip.domain.product.integration;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.Gender;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.repository.CountryRepository;
import com.talktrip.talktrip.global.s3.S3Uploader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class AdminProductIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @LocalServerPort private int port;

    @Autowired private MemberRepository memberRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CountryRepository countryRepository;

    private Member admin1;
    private Member admin2;
    private Member user;
    private Country country;
    private Product productA;
    private String admin1Token;
    private String admin2Token;
    private String userToken;

    private String baseUrl() { return "http://localhost:" + port + "/api"; }

    private HttpEntity<Void> auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return new HttpEntity<>(h);
    }

    private java.util.Map<String,Object> parseRoot(ResponseEntity<String> res) {
        try {
            String body = res.getBody();
            if (body == null || body.isBlank()) return java.util.Map.of();
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>(){});
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        country = Country.builder().id(100L).name("대한민국").continent("아시아").build();
        countryRepository.save(country);

        admin1 = Member.builder().accountEmail("admin1+"+ts+"@test.com").name("관리자1").nickname("A1").gender(Gender.M)
                .birthday(java.time.LocalDate.of(1990,1,1)).memberRole(MemberRole.A).memberState(MemberState.A).build();
        admin2 = Member.builder().accountEmail("admin2+"+ts+"@test.com").name("관리자2").nickname("A2").gender(Gender.F)
                .birthday(java.time.LocalDate.of(1992,2,2)).memberRole(MemberRole.A).memberState(MemberState.A).build();
        user = Member.builder().accountEmail("user+"+ts+"@test.com").name("유저").nickname("U1").gender(Gender.M)
                .birthday(java.time.LocalDate.of(1995,3,3)).memberRole(MemberRole.U).memberState(MemberState.A).build();
        memberRepository.save(admin1);
        memberRepository.save(admin2);
        memberRepository.save(user);

        productA = Product.builder().productName("관리자1 상품").description("desc").thumbnailImageUrl("t")
                .member(admin1).country(country).build();
        productRepository.save(productA);

        admin1Token = com.talktrip.talktrip.global.util.JWTUtil.generateToken(java.util.Map.of("email", admin1.getAccountEmail()), 60);
        admin2Token = com.talktrip.talktrip.global.util.JWTUtil.generateToken(java.util.Map.of("email", admin2.getAccountEmail()), 60);
        userToken = com.talktrip.talktrip.global.util.JWTUtil.generateToken(java.util.Map.of("email", user.getAccountEmail()), 60);
    }

    @AfterEach
    void tearDown() {
        try { productRepository.deleteAll(); } catch (Exception ignored) {}
        try { memberRepository.deleteAll(); } catch (Exception ignored) {}
        try { countryRepository.deleteAll(); } catch (Exception ignored) {}
    }

    @MockitoBean
    private S3Uploader s3Uploader;

    @Test
    @DisplayName("판매자 상품 목록 - ADMIN 토큰 200")
    void admin_list_ok() {
        String url = baseUrl() + "/admin/products?page=0&size=10&status=ACTIVE&sort=updatedAt&sort=desc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(admin1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("판매자 상품 목록 - 미지원 status → 400")
    void admin_list_invalid_status_400() {
        String url = baseUrl() + "/admin/products?page=0&size=10&status=INVALID&sort=updatedAt&sort=desc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(admin1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("판매자 상품 목록 - 미지원 정렬필드 → 400")
    void admin_list_unsupported_sort_400() {
        String url = baseUrl() + "/admin/products?page=0&size=10&status=ACTIVE&sort=notExists&sort=desc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(admin1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(400);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty() && root.get("message") != null) {
            org.assertj.core.api.Assertions.assertThat(String.valueOf(root.get("message"))).contains("Unsupported sort property");
        }
    }

    @Test
    @DisplayName("판매자 상품 목록 - 일반유저 토큰 → 403 ACCESS_DENIED")
    void admin_list_user_forbidden_403() {
        String url = baseUrl() + "/admin/products?page=0&size=10&status=ACTIVE";
        ResponseEntity<String> res = restTemplate.exchange(
                url, HttpMethod.GET, auth(userToken), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(403);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            assertThat(root.get("errorCode")).isEqualTo("ACCESS_DENIED");
        }
    }

    @Test
    @DisplayName("판매자 상품 상세 - 본인 소유 → 200")
    void admin_detail_ok() {
        String url = baseUrl() + "/admin/products/" + productA.getId();
        ResponseEntity<String> res = restTemplate.exchange(
                url, HttpMethod.GET, auth(admin1Token), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("판매자 상품 상세 - 타 관리자 소유 → 403 ACCESS_DENIED")
    void admin_detail_access_denied_403() {
        String url = baseUrl() + "/admin/products/" + productA.getId();
        ResponseEntity<String> res = restTemplate.exchange(
                url, HttpMethod.GET, auth(admin2Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(403);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            assertThat(root.get("errorCode")).isEqualTo("ACCESS_DENIED");
        }
    }

    @Test
    @DisplayName("판매자 상품 상세 - 존재하지 않음 → 404 PRODUCT_NOT_FOUND")
    void admin_detail_not_found_404() {
        String url = baseUrl() + "/admin/products/999999";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(admin1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(404);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            org.assertj.core.api.Assertions.assertThat(root.get("errorCode")).isEqualTo("PRODUCT_NOT_FOUND");
        }
    }

    private HttpEntity<MultiValueMap<String, Object>> multipartAuth(String token, MultiValueMap<String,Object> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(form, headers);
    }

    private static ByteArrayResource file(String filename) {
        return new ByteArrayResource("x".getBytes()) {
            @Override public String getFilename() { return filename; }
        };
    }

    @Test
    @DisplayName("상품 등록 - ADMIN 201")
    void admin_create_201() {
        org.mockito.Mockito.when(s3Uploader.upload(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("https://s3/mock.jpg");
        org.mockito.Mockito.when(s3Uploader.calculateHash(org.mockito.ArgumentMatchers.any()))
                .thenReturn("hash");

        String reqJson = "{" +
                "\"productName\":\"P\",\"description\":\"1234567890\",\"countryName\":\"대한민국\"," +
                "\"options\":[{\"startDate\":\"2030-01-01\",\"optionName\":\"opt\",\"stock\":1,\"price\":1,\"discountPrice\":1}]," +
                "\"hashtags\":[\"h\"]" +
                "}";
        MultiValueMap<String,Object> form = new LinkedMultiValueMap<>();
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        form.add("request", new org.springframework.http.HttpEntity<>(reqJson, jsonHeaders));
        form.add("thumbnailImage", file("t.jpg"));
        form.add("detailImages", file("d1.jpg"));

        ResponseEntity<String> res = restTemplate.postForEntity(baseUrl()+"/admin/products", multipartAuth(admin1Token, form), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    @DisplayName("상품 등록 - USER 403 ACCESS_DENIED")
    void admin_create_user_forbidden_403() {
        String reqJson = "{" +
                "\"productName\":\"P\",\"description\":\"1234567890\",\"countryName\":\"대한민국\"," +
                "\"options\":[{\"startDate\":\"2030-01-01\",\"optionName\":\"opt\",\"stock\":1,\"price\":1,\"discountPrice\":1}]," +
                "\"hashtags\":[\"h\"]" +
                "}";
        MultiValueMap<String,Object> form = new LinkedMultiValueMap<>();
        HttpHeaders jsonHeaders2 = new HttpHeaders();
        jsonHeaders2.setContentType(MediaType.APPLICATION_JSON);
        form.add("request", new org.springframework.http.HttpEntity<>(reqJson, jsonHeaders2));
        ResponseEntity<String> res = restTemplate.postForEntity(baseUrl()+"/admin/products", multipartAuth(userToken, form), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(403);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            org.assertj.core.api.Assertions.assertThat(root.get("errorCode")).isEqualTo("ACCESS_DENIED");
        }
    }

    @Test
    @DisplayName("상품 수정 - 본인 소유 200")
    void admin_update_200() {
        org.mockito.Mockito.when(s3Uploader.calculateHash(org.mockito.ArgumentMatchers.any())).thenReturn("hash");
        org.mockito.Mockito.when(s3Uploader.upload(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn("https://s3/x.jpg");

        String reqJson = "{" +
                "\"productName\":\"PX\",\"description\":\"1234567890\",\"countryName\":\"대한민국\"," +
                "\"options\":[{\"startDate\":\"2030-01-01\",\"optionName\":\"opt\",\"stock\":1,\"price\":1,\"discountPrice\":1}]," +
                "\"hashtags\":[\"h\"]" +
                "}";
        MultiValueMap<String,Object> form = new LinkedMultiValueMap<>();
        HttpHeaders jsonHeaders3 = new HttpHeaders();
        jsonHeaders3.setContentType(MediaType.APPLICATION_JSON);
        form.add("request", new org.springframework.http.HttpEntity<>(reqJson, jsonHeaders3));
        form.add("thumbnailImage", file("t.jpg"));

        ResponseEntity<String> res = restTemplate.exchange(baseUrl()+"/admin/products/"+productA.getId(), HttpMethod.PUT, multipartAuth(admin1Token, form), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("상품 수정 - 타 관리자 403 ACCESS_DENIED")
    void admin_update_access_denied_403() {
        String reqJson = "{" +
                "\"productName\":\"PX\",\"description\":\"1234567890\",\"countryName\":\"대한민국\"," +
                "\"options\":[{\"startDate\":\"2030-01-01\",\"optionName\":\"opt\",\"stock\":1,\"price\":1,\"discountPrice\":1}]," +
                "\"hashtags\":[\"h\"]" +
                "}";
        MultiValueMap<String,Object> form = new LinkedMultiValueMap<>();
        form.add("request", new org.springframework.http.HttpEntity<>(reqJson, new HttpHeaders()));
        ResponseEntity<String> res = restTemplate.exchange(baseUrl()+"/admin/products/"+productA.getId(), HttpMethod.PUT, multipartAuth(admin2Token, form), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(403);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            org.assertj.core.api.Assertions.assertThat(root.get("errorCode")).isEqualTo("ACCESS_DENIED");
        }
    }

    @Test
    @DisplayName("상품 수정 - 미존재 ID → 404 PRODUCT_NOT_FOUND")
    void admin_update_not_found_404() {
        String reqJson = "{" +
                "\"productName\":\"PX\",\"description\":\"1234567890\",\"countryName\":\"대한민국\"," +
                "\"options\":[{\"startDate\":\"2030-01-01\",\"optionName\":\"opt\",\"stock\":1,\"price\":1,\"discountPrice\":1}]," +
                "\"hashtags\":[\"h\"]" +
                "}";
        MultiValueMap<String,Object> form = new LinkedMultiValueMap<>();
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        form.add("request", new org.springframework.http.HttpEntity<>(reqJson, jsonHeaders));

        ResponseEntity<String> res = restTemplate.exchange(baseUrl()+"/admin/products/999999", HttpMethod.PUT, multipartAuth(admin1Token, form), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(404);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            org.assertj.core.api.Assertions.assertThat(root.get("errorCode")).isEqualTo("PRODUCT_NOT_FOUND");
        }
    }

    @Test
    @DisplayName("상품 삭제 - 본인 소유 204")
    void admin_delete_204() {
        ResponseEntity<String> res = restTemplate.exchange(baseUrl()+"/admin/products/"+productA.getId(), HttpMethod.DELETE, auth(admin1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    @DisplayName("상품 삭제 - 타 관리자 403 ACCESS_DENIED")
    void admin_delete_access_denied_403() {
        ResponseEntity<String> res = restTemplate.exchange(baseUrl()+"/admin/products/"+productA.getId(), HttpMethod.DELETE, auth(admin2Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(403);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            org.assertj.core.api.Assertions.assertThat(root.get("errorCode")).isEqualTo("ACCESS_DENIED");
        }
    }

    @Test
    @DisplayName("상품 삭제 - 미존재 ID → 404 PRODUCT_NOT_FOUND")
    void admin_delete_not_found_404() {
        ResponseEntity<String> res = restTemplate.exchange(baseUrl()+"/admin/products/999999", HttpMethod.DELETE, auth(admin1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(404);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            org.assertj.core.api.Assertions.assertThat(root.get("errorCode")).isEqualTo("PRODUCT_NOT_FOUND");
        }
    }

    @Test
    @DisplayName("상품 복구 - 삭제 상태에서 204")
    void admin_restore_204() {
        // soft delete 먼저
        ResponseEntity<String> del = restTemplate.exchange(baseUrl()+"/admin/products/"+productA.getId(), HttpMethod.DELETE, auth(admin1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(del.getStatusCode().value()).isEqualTo(204);

        ResponseEntity<String> res = restTemplate.postForEntity(baseUrl()+"/admin/products/"+productA.getId()+"/restore", auth(admin1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    @DisplayName("상품 복구 - 삭제되지 않은 상태 → 404 PRODUCT_NOT_FOUND")
    void admin_restore_not_deleted_404() {
        ResponseEntity<String> res = restTemplate.postForEntity(baseUrl()+"/admin/products/"+productA.getId()+"/restore", auth(admin1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(404);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            org.assertj.core.api.Assertions.assertThat(root.get("errorCode")).isEqualTo("PRODUCT_NOT_FOUND");
        }
    }

    @Test
    @DisplayName("상품 복구 - 미존재 ID → 404 PRODUCT_NOT_FOUND")
    void admin_restore_not_found_404() {
        ResponseEntity<String> res = restTemplate.postForEntity(baseUrl()+"/admin/products/999999/restore", auth(admin1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(404);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            org.assertj.core.api.Assertions.assertThat(root.get("errorCode")).isEqualTo("PRODUCT_NOT_FOUND");
        }
    }
}

