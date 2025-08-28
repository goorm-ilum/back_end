package com.talktrip.talktrip.domain.like.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.Gender;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.order.repository.OrderRepository;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.repository.CountryRepository;
import com.talktrip.talktrip.global.util.JWTUtil;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class LikeIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;
    @LocalServerPort private int port;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private Member member1;
    private Member member2;
    private Product product1;
    private Product product2;
    private Country country;
    private String baseUrl;
    private String member1Token;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        long ts = System.nanoTime();
        // 테스트 데이터 생성 (ID/이메일 중복 방지)
        country = Country.builder()
                .id(1L)
                .name("대한민국")
                .continent("아시아")
                .build();
        countryRepository.save(country);

        member1 = Member.builder()
                .accountEmail("user1+" + ts + "@test.com")
                .name("테스트유저1")
                .nickname("테스터1")
                .gender(Gender.M)
                .birthday(LocalDate.of(1990, 1, 1))
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(member1);

        member2 = Member.builder()
                .accountEmail("user2+" + ts + "@test.com")
                .name("테스트유저2")
                .nickname("테스터2")
                .gender(Gender.F)
                .birthday(LocalDate.of(1995, 5, 5))
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(member2);

        product1 = Product.builder()
                .productName("테스트 상품 1")
                .description("테스트 상품 1 설명")
                .thumbnailImageUrl("https://example.com/product1.jpg")
                .member(member1)
                .country(country)
                .build();
        productRepository.save(product1);

        product2 = Product.builder()
                .productName("테스트 상품 2")
                .description("테스트 상품 2 설명")
                .thumbnailImageUrl("https://example.com/product2.jpg")
                .member(member1)
                .country(country)
                .build();
        productRepository.save(product2);

        member1Token = JWTUtil.generateToken(java.util.Map.of("email", member1.getAccountEmail()), 60);
    }

    @AfterEach
    void tearDown() {
        try { likeRepository.deleteAll(); } catch (Exception ignored) {}
        try { productRepository.deleteAll(); } catch (Exception ignored) {}
        try { memberRepository.deleteAll(); } catch (Exception ignored) {}
        try { countryRepository.deleteAll(); } catch (Exception ignored) {}
    }

    // ==== Controller 호출 기반 통합테스트 보강 ====
    private HttpEntity<Void> auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return new HttpEntity<>(h);
    }

    private java.util.Map<String,Object> parseRoot(ResponseEntity<String> res) {
        try {
            String body = res.getBody();
            if (body == null || body.isBlank()) return java.util.Map.of();
            return objectMapper.readValue(body, new TypeReference<>() {
            });
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private java.util.List<java.util.Map<String,Object>> parseContent(ResponseEntity<String> res) {
        try {
            String body = res.getBody();
            if (body == null || body.isBlank()) return java.util.List.of();
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(body);
            com.fasterxml.jackson.databind.JsonNode content = root.path("content");
            if (content.isMissingNode() || content.isNull()) return java.util.List.of();
            return objectMapper.convertValue(content, new TypeReference<>() {
            });
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private Order createOrder(Member m) {
        Order o = Order.builder()
                .member(m)
                .orderStatus(OrderStatus.SUCCESS)
                .orderCode("LIKE-" + System.nanoTime())
                .orderDate(java.time.LocalDate.now())
                .totalPrice(0)
                .build();
        return orderRepository.save(o);
    }

    @Test @DisplayName("좋아요 토글 - 200")
    void controller_toggle_like_then_unlike() {
        String url = baseUrl + "/products/" + product1.getId() + "/like";
        ResponseEntity<String> first = restTemplate.exchange(url, HttpMethod.POST, auth(member1Token), String.class);
        assertThat(first.getStatusCode().is2xxSuccessful()).isTrue();
        ResponseEntity<String> second = restTemplate.exchange(url, HttpMethod.POST, auth(member1Token), String.class);
        assertThat(second.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test @DisplayName("좋아요 목록 - 페이징/정렬")
    void controller_my_likes_paging_sort() {
        String likeA = baseUrl + "/products/" + product1.getId() + "/like";
        String likeB = baseUrl + "/products/" + product2.getId() + "/like";
        restTemplate.exchange(likeA, HttpMethod.POST, auth(member1Token), String.class);
        restTemplate.exchange(likeB, HttpMethod.POST, auth(member1Token), String.class);

        String url = baseUrl + "/me/likes?page=0&size=1&sort=updatedAt&sort=desc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        java.util.Map<String,Object> root = parseRoot(res);
        assertThat(((Number)root.get("totalElements")).longValue()).isEqualTo(2L);
    }

    @Test @DisplayName("좋아요 목록 - averageReviewStar 정렬 asc/desc")
    void controller_my_likes_sort_by_avgStar() {
        // 준비: 리뷰 생성 (product1: 5.0, product2: 3.0)
        Order o1 = createOrder(member1);
        Order o2 = createOrder(member1);
        Review r1 = Review.builder().product(product1).member(member1).order(o1).reviewStar(5.0).comment("좋음").build();
        Review r2 = Review.builder().product(product2).member(member1).order(o2).reviewStar(3.0).comment("보통").build();
        reviewRepository.save(r1);
        reviewRepository.save(r2);

        // 좋아요 추가
        restTemplate.exchange(baseUrl + "/products/" + product1.getId() + "/like", HttpMethod.POST, auth(member1Token), String.class);
        restTemplate.exchange(baseUrl + "/products/" + product2.getId() + "/like", HttpMethod.POST, auth(member1Token), String.class);

        // desc: product1(5.0) 먼저
        String desc = baseUrl + "/me/likes?page=0&size=10&sort=averageReviewStar&sort=desc";
        ResponseEntity<String> rDesc = restTemplate.exchange(desc, HttpMethod.GET, auth(member1Token), String.class);
        assertThat(rDesc.getStatusCode().is2xxSuccessful()).isTrue();
        java.util.List<java.util.Map<String,Object>> contentD = parseContent(rDesc);
        assertThat(((Number)contentD.get(0).get("averageReviewStar")).doubleValue()).isGreaterThanOrEqualTo(((Number)contentD.get(1).get("averageReviewStar")).doubleValue());

        // asc: product2(3.0) 먼저
        String asc = baseUrl + "/me/likes?page=0&size=10&sort=averageReviewStar&sort=asc";
        ResponseEntity<String> rAsc = restTemplate.exchange(asc, HttpMethod.GET, auth(member1Token), String.class);
        assertThat(rAsc.getStatusCode().is2xxSuccessful()).isTrue();
        java.util.List<java.util.Map<String,Object>> contentA = parseContent(rAsc);
        assertThat(((Number)contentA.get(0).get("averageReviewStar")).doubleValue()).isLessThanOrEqualTo(((Number)contentA.get(1).get("averageReviewStar")).doubleValue());
    }

    @Test @DisplayName("좋아요 목록 - updatedAt 정렬 asc/desc")
    void controller_my_likes_sort_by_updatedAt() {
        // 좋아요 추가 (product1 먼저, 이어서 product2 생성되어 기본 updatedAt은 product2가 더 최근)
        restTemplate.exchange(baseUrl + "/products/" + product1.getId() + "/like", HttpMethod.POST, auth(member1Token), String.class);
        restTemplate.exchange(baseUrl + "/products/" + product2.getId() + "/like", HttpMethod.POST, auth(member1Token), String.class);

        // desc: 더 최근(updatedAt)인 product2가 먼저
        String desc = baseUrl + "/me/likes?page=0&size=10&sort=updatedAt&sort=desc";
        ResponseEntity<String> rDesc = restTemplate.exchange(desc, HttpMethod.GET, auth(member1Token), String.class);
        assertThat(rDesc.getStatusCode().is2xxSuccessful()).isTrue();
        java.util.List<java.util.Map<String,Object>> contentD = parseContent(rDesc);
        assertThat(contentD).hasSize(2);
        assertThat(String.valueOf(contentD.get(0).get("productName"))).isEqualTo("테스트 상품 2");
        assertThat(String.valueOf(contentD.get(1).get("productName"))).isEqualTo("테스트 상품 1");

        // asc: 더 오래된(updatedAt) product1이 먼저
        String asc = baseUrl + "/me/likes?page=0&size=10&sort=updatedAt&sort=asc";
        ResponseEntity<String> rAsc = restTemplate.exchange(asc, HttpMethod.GET, auth(member1Token), String.class);
        assertThat(rAsc.getStatusCode().is2xxSuccessful()).isTrue();
        java.util.List<java.util.Map<String,Object>> contentA = parseContent(rAsc);
        assertThat(contentA).hasSize(2);
        assertThat(String.valueOf(contentA.get(0).get("productName"))).isEqualTo("테스트 상품 1");
        assertThat(String.valueOf(contentA.get(1).get("productName"))).isEqualTo("테스트 상품 2");
    }

    @Test @DisplayName("좋아요 목록 - 미지원 정렬필드 → 400")
    void controller_my_likes_unsupportedSort_4xx() {
        String url = baseUrl + "/me/likes?page=0&size=10&sort=notExists&sort=desc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty() && root.get("message") != null) {
            assertThat(String.valueOf(root.get("message"))).contains("Unsupported sort property");
        }
    }

    @Test @DisplayName("좋아요 목록 - empty 결과")
    void controller_my_likes_empty() {
        String url = baseUrl + "/me/likes?page=0&size=10";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        java.util.Map<String,Object> root = parseRoot(res);
        assertThat(((Number)root.get("totalElements")).longValue()).isEqualTo(0L);
    }

    @Test @DisplayName("좋아요 토글 - 존재하지 않는 상품 → 404")
    void controller_toggle_invalid_product_404() {
        String url = baseUrl + "/products/999999/like";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, auth(member1Token), String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(404);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            assertThat(root.get("errorCode")).isEqualTo("PRODUCT_NOT_FOUND");
        }
    }

    @Test
    @DisplayName("좋아요를 삭제한다(컨트롤러 토글 재호출)")
    void deleteLike_Success() {
        String url = baseUrl + "/products/" + product1.getId() + "/like";
        ResponseEntity<String> like = restTemplate.exchange(url, HttpMethod.POST, auth(member1Token), String.class);
        assertThat(like.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(likeRepository.existsByProductIdAndMemberId(product1.getId(), member1.getId())).isTrue();

        ResponseEntity<String> unlike = restTemplate.exchange(url, HttpMethod.POST, auth(member1Token), String.class);
        assertThat(unlike.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(likeRepository.existsByProductIdAndMemberId(product1.getId(), member1.getId())).isFalse();
    }
}
