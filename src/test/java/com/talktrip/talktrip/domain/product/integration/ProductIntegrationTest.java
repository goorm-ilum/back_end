package com.talktrip.talktrip.domain.product.integration;

import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.Gender;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.entity.OrderItem;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.order.repository.OrderItemRepository;
import com.talktrip.talktrip.domain.order.repository.OrderRepository;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.repository.ProductOptionRepository;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.repository.CountryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class ProductIntegrationTest {

    @Autowired
    private org.springframework.boot.test.web.client.TestRestTemplate restTemplate;
    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;
    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Member member1;
    private Member member2;
    private Country country1;
    private Country country2;
    private Product product1;
    private Product product2;
    private Product product3;
    private ProductOption option1;
    private ProductOption option2;
    private ProductOption option3;
    private Order order1;
    private Order order2;
    private Order order3;
    private Review review1;
    private Review review2;
    private Review review3;
    private String baseUrl;
    private String member1Token;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        // 테스트 데이터 생성 (ID/이메일 중복 방지)
        long ts = System.nanoTime();
        country1 = Country.builder()
                .id(1L)
                .name("대한민국")
                .continent("아시아")
                .build();
        country1 = countryRepository.save(country1);

        country2 = Country.builder()
                .id(2L)
                .name("일본")
                .continent("아시아")
                .build();
        country2 = countryRepository.save(country2);

        member1 = Member.builder()
                .accountEmail("test1+" + ts + "@test.com")
                .name("테스트유저1")
                .nickname("테스터1")
                .gender(Gender.M)
                .birthday(LocalDate.of(1990, 1, 1))
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(member1);

        member2 = Member.builder()
                .accountEmail("test2+" + ts + "@test.com")
                .name("테스트유저2")
                .nickname("테스터2")
                .gender(Gender.F)
                .birthday(LocalDate.of(1995, 5, 5))
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(member2);

        product1 = Product.builder()
                .productName("제주도 여행")
                .description("아름다운 제주도 여행 상품입니다")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .member(member1)
                .country(country1)
                .deleted(false)
                .build();
        productRepository.save(product1);

        product2 = Product.builder()
                .productName("서울 도시 여행")
                .description("서울의 다양한 관광지를 둘러보는 상품")
                .thumbnailImageUrl("https://example.com/seoul.jpg")
                .member(member1)
                .country(country1)
                .deleted(false)
                .build();
        productRepository.save(product2);

        product3 = Product.builder()
                .productName("도쿄 여행")
                .description("일본 도쿄의 매력을 느껴보세요")
                .thumbnailImageUrl("https://example.com/tokyo.jpg")
                .member(member2)
                .country(country2)
                .deleted(false)
                .build();
        productRepository.save(product3);

        // 상품 옵션 생성 및 저장
        option1 = ProductOption.builder()
                .product(product1)
                .optionName("기본 패키지")
                .startDate(LocalDate.now().plusDays(1))
                .stock(10)
                .price(100000)
                .discountPrice(90000)
                .build();
        productOptionRepository.save(option1);

        option2 = ProductOption.builder()
                .product(product2)
                .optionName("프리미엄 패키지")
                .startDate(LocalDate.now().plusDays(2))
                .stock(5)
                .price(150000)
                .discountPrice(130000)
                .build();
        productOptionRepository.save(option2);

        option3 = ProductOption.builder()
                .product(product3)
                .optionName("스탠다드 패키지")
                .startDate(LocalDate.now().plusDays(3))
                .stock(8)
                .price(200000)
                .discountPrice(180000)
                .build();
        productOptionRepository.save(option3);

        // 주문 생성
        order1 = Order.builder()
                .member(member2)
                .orderDate(LocalDate.now().minusDays(7))
                .totalPrice(100000)
                .orderStatus(OrderStatus.SUCCESS)
                .orderCode("ORDER-001")
                .build();
        orderRepository.save(order1);

        OrderItem orderItem1 = OrderItem.builder()
                .order(order1)
                .productId(product1.getId())
                .quantity(1)
                .price(100000)
                .build();
        orderItemRepository.save(orderItem1);

        order2 = Order.builder()
                .member(member1)
                .orderDate(LocalDate.now().minusDays(5))
                .totalPrice(150000)
                .orderStatus(OrderStatus.SUCCESS)
                .orderCode("ORDER-002")
                .build();
        orderRepository.save(order2);

        OrderItem orderItem2 = OrderItem.builder()
                .order(order2)
                .productId(product1.getId())
                .quantity(1)
                .price(150000)
                .build();
        orderItemRepository.save(orderItem2);

        order3 = Order.builder()
                .member(member2)
                .orderDate(LocalDate.now().minusDays(3))
                .totalPrice(200000)
                .orderStatus(OrderStatus.SUCCESS)
                .orderCode("ORDER-003")
                .build();
        orderRepository.save(order3);

        OrderItem orderItem3 = OrderItem.builder()
                .order(order3)
                .productId(product2.getId())
                .quantity(1)
                .price(200000)
                .build();
        orderItemRepository.save(orderItem3);

        // 리뷰 생성 및 저장
        review1 = Review.builder()
                .product(product1)
                .member(member2)
                .order(order1)
                .reviewStar(4.5)
                .comment("정말 좋은 여행이었습니다!")
                .build();
        reviewRepository.save(review1);

        review2 = Review.builder()
                .product(product1)
                .member(member1)
                .order(order2)
                .reviewStar(5.0)
                .comment("완벽한 여행이었어요!")
                .build();
        reviewRepository.save(review2);

        review3 = Review.builder()
                .product(product2)
                .member(member2)
                .order(order3)
                .reviewStar(3.5)
                .comment("괜찮았습니다.")
                .build();
        reviewRepository.save(review3);

        // 좋아요 생성 및 저장
        Like like1 = Like.builder()
                .productId(product1.getId())
                .memberId(member2.getId())
                .build();
        likeRepository.save(like1);

        Like like2 = Like.builder()
                .productId(product2.getId())
                .memberId(member1.getId())
                .build();
        likeRepository.save(like2);

        member1Token = com.talktrip.talktrip.global.util.JWTUtil.generateToken(java.util.Map.of("email", member1.getAccountEmail()), 60);
    }

    @AfterEach
    void tearDown() {
        try { likeRepository.deleteAll(); } catch (Exception ignored) {}
        try { reviewRepository.deleteAll(); } catch (Exception ignored) {}
        try { orderItemRepository.deleteAll(); } catch (Exception ignored) {}
        try { orderRepository.deleteAll(); } catch (Exception ignored) {}
        try { productOptionRepository.deleteAll(); } catch (Exception ignored) {}
        try { productRepository.deleteAll(); } catch (Exception ignored) {}
        try { memberRepository.deleteAll(); } catch (Exception ignored) {}
        try { countryRepository.deleteAll(); } catch (Exception ignored) {}
    }

    // ==== Controller 호출 기반 통합테스트 (ProductController) ====
    private
    HttpEntity<Void> auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return new HttpEntity<>(h);
    }

    private java.util.Map<String,Object> parseRoot(ResponseEntity<String> res) {
        try {
            String body = res.getBody();
            if (body == null || body.isBlank()) return java.util.Map.of();
            return objectMapper.readValue(body, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>(){});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private java.util.List<java.util.Map<String,Object>> parseContent(ResponseEntity<String> res) {
        try {
            String body = res.getBody();
            if (body == null || body.isBlank()) return java.util.List.of();
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(body);
            com.fasterxml.jackson.databind.JsonNode content = root.path("content");
            if (content.isMissingNode() || content.isNull()) return java.util.List.of();
            return objectMapper.convertValue(content, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String,Object>>>(){});
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    @DisplayName("상품 목록 검색 - 키워드 + 정렬/페이징 200")
    void controller_getProducts_ok() {
        String url = baseUrl + "/products?keyword=제주도&countryName=전체&page=0&size=10&sort=updatedAt&sort=desc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        java.util.Map<String,Object> root = parseRoot(res);
        org.assertj.core.api.Assertions.assertThat(((Number)root.get("totalElements")).longValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("상품 목록 검색 - 잘못된 정렬 필드 4xx")
    void controller_getProducts_invalidSort() {
        String url = baseUrl + "/products?keyword=&countryName=전체&page=0&size=10&sort=notExists&sort=asc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(400);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty() && root.get("message") != null) {
            org.assertj.core.api.Assertions.assertThat(String.valueOf(root.get("message"))).contains("Unsupported sort property");
        }
    }

    @Test
    @DisplayName("상품 상세 조회 - 200")
    void controller_getProductDetail_ok() {
        String url = baseUrl + "/products/" + product1.getId() + "?page=0&size=1&sort=updatedAt&sort=desc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("상품 상세 조회 - 상품 없음 4xx")
    void controller_getProductDetail_notFound() {
        String url = baseUrl + "/products/999999?page=0&size=1&sort=updatedAt&sort=desc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(404);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            org.assertj.core.api.Assertions.assertThat(root.get("errorCode")).isEqualTo("PRODUCT_NOT_FOUND");
        }
    }

    @Test
    @DisplayName("상품 상세 조회 - 잘못된 정렬 필드 4xx")
    void controller_getProductDetail_invalidSort() {
        String url = baseUrl + "/products/" + product1.getId() + "?page=0&size=1&sort=notExists&sort=asc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(400);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty() && root.get("message") != null) {
            org.assertj.core.api.Assertions.assertThat(String.valueOf(root.get("message"))).contains("Unsupported sort property");
        }
    }

    @Test
    @DisplayName("상품 목록 검색 - 국가 필터 200")
    void controller_getProducts_country_ok() {
        String url = baseUrl + "/products?countryName=대한민국&page=0&size=10&sort=updatedAt&sort=desc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        java.util.Map<String,Object> root = parseRoot(res);
        org.assertj.core.api.Assertions.assertThat(((Number)root.get("totalElements")).longValue()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("상품 목록 검색 - 잘못된 국가 4xx")
    void controller_getProducts_invalid_country() {
        String url = baseUrl + "/products?countryName=존재하지않는국가&page=0&size=10";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(404);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            org.assertj.core.api.Assertions.assertThat(root.get("errorCode")).isEqualTo("COUNTRY_NOT_FOUND");
        }
    }

    @Test
    @DisplayName("상품 목록 검색 - page 유효성 4xx")
    void controller_getProducts_invalid_page() {
        String url = baseUrl + "/products?countryName=전체&page=-1&size=10";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @DisplayName("상품 목록 검색 - size 유효성 4xx")
    void controller_getProducts_invalid_size() {
        String url = baseUrl + "/products?countryName=전체&page=0&size=0";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @DisplayName("상품 목록 검색 - averageReviewStar 정렬 200")
    void controller_getProducts_sort_averageReviewStar() {
        String url = baseUrl + "/products?countryName=전체&sort=averageReviewStar&sort=desc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("상품 목록 검색 - discountPrice 정렬 200")
    void controller_getProducts_sort_discountPrice() {
        String url = baseUrl + "/products?countryName=전체&sort=discountPrice&sort=asc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(member1Token), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("상품 목록 검색 - 다중 키워드(제목 AND) 200")
    void controller_getProducts_multi_keyword_ok() {
        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/products")
                .queryParam("keyword", "서울 여행")
                .queryParam("countryName", "대한민국")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sort", "updatedAt")
                .queryParam("sort", "desc")
                .encode(java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toUri();

        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, auth(member1Token), String.class);

        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        java.util.List<java.util.Map<String,Object>> content = parseContent(res);
        boolean matched = content.stream().anyMatch(m -> {
            String name = String.valueOf(m.get("productName"));
            return name.contains("서울") && name.contains("여행");
        });
        if (!matched) {
            System.out.println("[DEBUG] multi keyword body: " + res.getBody());
        }
        assertThat(matched).isTrue();
    }

    @Test
    @DisplayName("AI 검색 - question 필수, member null로 4xx")
    void controller_aiSearch_4xx() {
        String url = baseUrl + "/products/aisearch?question=제주도%20추천&page=0&size=10";
        org.springframework.http.ResponseEntity<String> res = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, new org.springframework.http.HttpEntity<>(new org.springframework.http.HttpHeaders()), String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(404);
        java.util.Map<String,Object> root = parseRoot(res);
        if (!root.isEmpty()) {
            org.assertj.core.api.Assertions.assertThat(root.get("errorCode")).isEqualTo("USER_NOT_FOUND");
        }
    }

    @Test
    @DisplayName("AI 검색 - 필수 파라미터 누락 → 400")
    void controller_aiSearch_missing_required_param_400() {
        String url = baseUrl + "/products/aisearch?page=0&size=10";
        org.springframework.http.ResponseEntity<String> res = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, new org.springframework.http.HttpEntity<>(new org.springframework.http.HttpHeaders()), String.class);
        org.assertj.core.api.Assertions.assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }
}
