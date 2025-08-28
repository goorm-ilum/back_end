package com.talktrip.talktrip.domain.review.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.Gender;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.entity.OrderItem;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.order.repository.OrderRepository;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.dto.request.ReviewRequest;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.repository.CountryRepository;
import com.talktrip.talktrip.global.util.JWTUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class ReviewIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    // setup/teardown용 리포지토리
    @Autowired private MemberRepository memberRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CountryRepository countryRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ReviewRepository reviewRepository;

    @LocalServerPort private int port;
    @Autowired private ObjectMapper objectMapper;

    private String baseUrl;

    // 주요 테스트 데이터
    private Member seller;
    private Member reviewer;
    private Member otherUser;
    private Product product1;
    private Product product2;
    private Review review1;
    private Review review2;

    private String code1; // reviewer의 과거 주문(이미 리뷰 작성됨)
    private String code2; // otherUser의 과거 주문

    // 폼/생성용 추가 주문들
    private Long orderSuccessId;         // SUCCESS + item 존재
    private Long orderPendingId;         // PENDING + item 존재
    private Long orderEmptyId;           // SUCCESS + item 없음
    private Long orderInvalidProductId;  // SUCCESS + item 존재(없는 productId)

    private String sellerToken;
    private String reviewerToken;

    /* ==========================
     * Common Helpers
     * ========================== */

    private HttpEntity<Void> auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return new HttpEntity<>(h);
    }

    private HttpEntity<String> authJson(String token, Object body) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setBearerAuth(token);
            h.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return new HttpEntity<>(objectMapper.writeValueAsString(body), h);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> parseRoot(ResponseEntity<String> res) {
        try {
            String body = res.getBody();
            if (body == null || body.isBlank()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("응답 파싱 실패", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseContent(ResponseEntity<String> res) {
        Map<String, Object> root = parseRoot(res);
        return (List<Map<String, Object>>) root.getOrDefault("content", List.of());
    }

    private long asLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(v));
    }

    /* ==========================
     * Setup / Teardown
     * ========================== */

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";

        long ts = System.nanoTime();
        String sellerEmail = "seller+" + ts + "@test.com";
        String reviewerEmail = "user2+" + ts + "@test.com";
        String otherEmail = "user3+" + ts + "@test.com";
        code1 = "ORD-" + ts + "-001";
        code2 = "ORD-" + ts + "-002";

        Country kr = Country.builder().id(1L).name("대한민국").continent("아시아").build();
        countryRepository.save(kr);

        seller = Member.builder()
                .accountEmail(sellerEmail).name("판매자").nickname("셀러")
                .gender(Gender.M).birthday(LocalDate.of(1990,1,1))
                .memberRole(MemberRole.U).memberState(MemberState.A).build();
        memberRepository.save(seller);

        reviewer = Member.builder()
                .accountEmail(reviewerEmail).name("사용자2").nickname("리뷰어")
                .gender(Gender.F).birthday(LocalDate.of(1995,5,5))
                .memberRole(MemberRole.U).memberState(MemberState.A).build();
        memberRepository.save(reviewer);

        otherUser = Member.builder()
                .accountEmail(otherEmail).name("사용자3").nickname("기타")
                .gender(Gender.F).birthday(LocalDate.of(1997,5,5))
                .memberRole(MemberRole.U).memberState(MemberState.A).build();
        memberRepository.save(otherUser);

        product1 = Product.builder()
                .productName("제주도 여행").description("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .member(seller).country(kr).build();
        productRepository.save(product1);

        product2 = Product.builder()
                .productName("서울 여행").description("서울 투어")
                .thumbnailImageUrl("https://example.com/seoul.jpg")
                .member(seller).country(kr).build();
        productRepository.save(product2);

        // reviewer의 과거 주문 + 작성된 리뷰
        Order order1 = Order.builder()
                .member(reviewer).orderDate(LocalDate.now().minusDays(7))
                .totalPrice(100000).orderCode(code1)
                .orderStatus(OrderStatus.SUCCESS)
                .build();
        orderRepository.save(order1);

        // otherUser의 과거 주문 + 작성된 리뷰
        Order order2 = Order.builder()
                .member(otherUser).orderDate(LocalDate.now().minusDays(5))
                .totalPrice(150000).orderCode(code2).build();
        orderRepository.save(order2);

        review1 = Review.builder()
                .member(reviewer).product(product1).order(order1)
                .reviewStar(4.5).comment("좋은 여행이었습니다").build();
        reviewRepository.save(review1);

        review2 = Review.builder()
                .member(otherUser).product(product1).order(order2)
                .reviewStar(5.0).comment("최고의 여행이었습니다!").build();
        reviewRepository.save(review2);

        // JWT 발급
        sellerToken   = JWTUtil.generateToken(Map.of("email", sellerEmail), 1800);
        reviewerToken = JWTUtil.generateToken(Map.of("email", reviewerEmail), 1800);

        // 리뷰 작성 가능/불가 케이스용 주문들
        Order orderSuccess = Order.builder()
                .member(reviewer).orderDate(LocalDate.now().minusDays(2))
                .totalPrice(120000).orderStatus(OrderStatus.SUCCESS)
                .orderCode("ORD-" + ts + "-S").build();
        OrderItem oiS = OrderItem.createOrderItem(
                product1.getId(), product1.getProductName(), product1.getThumbnailImageUrl(),
                100000, null, null, 0, 0, LocalDate.now().plusDays(10), 1, 100000);
        orderSuccess.addOrderItem(oiS);
        orderRepository.save(orderSuccess);
        orderSuccessId = orderSuccess.getId();

        Order orderPending = Order.builder()
                .member(reviewer).orderDate(LocalDate.now().minusDays(1))
                .totalPrice(90000) // status default = PENDING
                .orderCode("ORD-" + ts + "-P").build();
        OrderItem oiP = OrderItem.createOrderItem(
                product1.getId(), product1.getProductName(), product1.getThumbnailImageUrl(),
                100000, null, null, 0, 0, LocalDate.now().plusDays(11), 1, 90000);
        orderPending.addOrderItem(oiP);
        orderRepository.save(orderPending);
        orderPendingId = orderPending.getId();

        Order orderEmpty = Order.builder()
                .member(reviewer).orderDate(LocalDate.now().minusDays(1))
                .totalPrice(80000).orderStatus(OrderStatus.SUCCESS)
                .orderCode("ORD-" + ts + "-E").build(); // item 없음
        orderRepository.save(orderEmpty);
        orderEmptyId = orderEmpty.getId();

        Order orderInvalidProduct = Order.builder()
                .member(reviewer).orderDate(LocalDate.now().minusDays(1))
                .totalPrice(80000).orderStatus(OrderStatus.SUCCESS)
                .orderCode("ORD-" + ts + "-X").build();
        OrderItem oiX = OrderItem.createOrderItem(
                -9999L, "없음", null, 0, null, null, 0, 0,
                LocalDate.now().plusDays(12), 1, 0);
        orderInvalidProduct.addOrderItem(oiX);
        orderRepository.save(orderInvalidProduct);
        orderInvalidProductId = orderInvalidProduct.getId();
    }

    @AfterEach
    void tearDown() {
        // 리뷰 → 주문 → 상품 → 회원 → 국가 순서로 방어적 삭제
        try { if (review1 != null && review1.getId() != null) reviewRepository.deleteById(review1.getId()); } catch (Exception ignored) {}
        try { if (review2 != null && review2.getId() != null) reviewRepository.deleteById(review2.getId()); } catch (Exception ignored) {}
        try { orderRepository.findByOrderCode(code1).ifPresent(o -> orderRepository.deleteById(o.getId())); } catch (Exception ignored) {}
        try { orderRepository.findByOrderCode(code2).ifPresent(o -> orderRepository.deleteById(o.getId())); } catch (Exception ignored) {}
        try { if (product1 != null && product1.getId() != null) productRepository.deleteById(product1.getId()); } catch (Exception ignored) {}
        try { if (product2 != null && product2.getId() != null) productRepository.deleteById(product2.getId()); } catch (Exception ignored) {}
        try { if (seller != null && seller.getId() != null)   memberRepository.deleteById(seller.getId()); } catch (Exception ignored) {}
        try { if (reviewer != null && reviewer.getId() != null) memberRepository.deleteById(reviewer.getId()); } catch (Exception ignored) {}
        try { if (otherUser != null && otherUser.getId() != null) memberRepository.deleteById(otherUser.getId()); } catch (Exception ignored) {}
        try { countryRepository.findById(1L).ifPresent(countryRepository::delete); } catch (Exception ignored) {}
    }

    /* ============================================================
     * 1) 관리자 상품별 리뷰 조회 (/api/admin/products/{productId}/reviews)
     *    - RepositoryImpl.findByProductIdWithPaging 커버
     * ============================================================ */

    @Test @DisplayName("관리자 상품별 리뷰 - 기본 정렬(updatedAt,desc)")
    void admin_defaultSort_updatedAtDesc() {
        String url = baseUrl + "/admin/products/" + product1.getId() + "/reviews?page=0&size=10";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        List<Map<String, Object>> content = parseContent(res);
        assertThat(content).hasSize(2);
        assertThat(asLong(content.get(0).get("reviewId"))).isEqualTo(review2.getId());
        assertThat(asLong(content.get(1).get("reviewId"))).isEqualTo(review1.getId());
    }

    @Test @DisplayName("관리자 상품별 리뷰 - reviewStar ASC")
    void admin_sort_reviewStarAsc() {
        String url = baseUrl + "/admin/products/" + product1.getId() + "/reviews?page=0&size=10&sort=reviewStar&sort=asc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        List<Map<String, Object>> c = parseContent(res);
        assertThat(c).hasSize(2);
        assertThat(c.get(0).get("reviewStar")).isEqualTo(4.5);
        assertThat(c.get(1).get("reviewStar")).isEqualTo(5.0);
    }

    @Test @DisplayName("관리자 상품별 리뷰 - reviewStar DESC")
    void admin_sort_reviewStarDesc() {
        String url = baseUrl + "/admin/products/" + product1.getId() + "/reviews?page=0&size=10&sort=reviewStar&sort=desc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        List<Map<String, Object>> c = parseContent(res);
        assertThat(c).hasSize(2);
        assertThat(c.get(0).get("reviewStar")).isEqualTo(5.0);
        assertThat(c.get(1).get("reviewStar")).isEqualTo(4.5);
    }

    @Test @DisplayName("관리자 상품별 리뷰 - updatedAt ASC")
    void admin_sort_updatedAtAsc() {
        String url = baseUrl + "/admin/products/" + product1.getId() + "/reviews?page=0&size=10&sort=updatedAt&sort=asc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        List<Map<String, Object>> c = parseContent(res);
        assertThat(c).hasSize(2);
        assertThat(asLong(c.get(0).get("reviewId"))).isEqualTo(review1.getId());
        assertThat(asLong(c.get(1).get("reviewId"))).isEqualTo(review2.getId());
    }

    @Test @DisplayName("관리자 상품별 리뷰 - updatedAt DESC")
    void admin_sort_updatedAtDesc() {
        String url = baseUrl + "/admin/products/" + product1.getId() + "/reviews?page=0&size=10&sort=updatedAt&sort=desc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        List<Map<String, Object>> c = parseContent(res);
        assertThat(c).hasSize(2);
        assertThat(asLong(c.get(0).get("reviewId"))).isEqualTo(review2.getId());
        assertThat(asLong(c.get(1).get("reviewId"))).isEqualTo(review1.getId());
    }

    @Test @DisplayName("관리자 상품별 리뷰 - 미지원 정렬필드 → 400")
    void admin_unsupportedSortProperty_400() {
        String url = baseUrl + "/admin/products/" + product1.getId() + "/reviews?page=0&size=10&sort=notExistsProperty&sort=asc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("관리자 상품별 리뷰 - 정렬 방향 INVALID → 400")
    void admin_invalidDirection_400() {
        String url = baseUrl + "/admin/products/" + product1.getId() + "/reviews?page=0&size=10&sort=updatedAt&sort=INVALID";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("관리자 상품별 리뷰 - 타 사용자(비소유자) 접근 → 403")
    void admin_accessDenied_403() {
        String url = baseUrl + "/admin/products/" + product1.getId() + "/reviews?page=0&size=10";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        Map<String,Object> err = parseRoot(res);
        assertThat(err.get("errorCode")).isEqualTo("ACCESS_DENIED");
    }

    @Test @DisplayName("관리자 상품별 리뷰 - 상품 없음 → 404")
    void admin_productNotFound_404() {
        String url = baseUrl + "/admin/products/999999/reviews?page=0&size=10";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        Map<String,Object> err = parseRoot(res);
        assertThat(err.get("errorCode")).isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test @DisplayName("관리자 상품별 리뷰 - 페이징(total=2 → size=1로 두 페이지)")
    void admin_paging_totals() {
        String url0 = baseUrl + "/admin/products/" + product1.getId() + "/reviews?page=0&size=1";
        String url1 = baseUrl + "/admin/products/" + product1.getId() + "/reviews?page=1&size=1";
        ResponseEntity<String> res0 = restTemplate.exchange(url0, HttpMethod.GET, auth(sellerToken), String.class);
        ResponseEntity<String> res1 = restTemplate.exchange(url1, HttpMethod.GET, auth(sellerToken), String.class);
        Map<String,Object> root0 = parseRoot(res0);
        Map<String,Object> root1 = parseRoot(res1);
        assertThat(((Number)root0.get("totalElements")).longValue()).isEqualTo(2L);
        assertThat(((Number)root1.get("totalElements")).longValue()).isEqualTo(2L);
        assertThat(((Number)root0.get("totalPages")).intValue()).isEqualTo(2);
        assertThat(((Number)root1.get("totalPages")).intValue()).isEqualTo(2);
    }

    @Test @DisplayName("관리자 상품별 리뷰 - 리뷰 없음(total=0)")
    void admin_emptyTotal_zero() {
        String url = baseUrl + "/admin/products/" + product2.getId() + "/reviews?page=0&size=10";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        Map<String,Object> root = parseRoot(res);
        assertThat(((Number)root.get("totalElements")).longValue()).isEqualTo(0L);
        assertThat(((Number)root.get("totalPages")).intValue()).isEqualTo(0);
        assertThat(parseContent(res)).isEmpty();
    }

    @Test @DisplayName("관리자 상품별 리뷰 - 응답 필드 스키마 확인")
    void admin_responseSchema_ok() {
        String url = baseUrl + "/admin/products/" + product1.getId() + "/reviews?page=0&size=10&sort=updatedAt&sort=desc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        List<Map<String, Object>> c = parseContent(res);
        assertThat(c).isNotEmpty();
        Map<String, Object> first = c.get(0);
        assertThat(first.get("productName")).isEqualTo(product1.getProductName());
        assertThat(first.get("thumbnailImageUrl")).isEqualTo(product1.getThumbnailImageUrl());
        assertThat(first.get("comment")).isInstanceOf(String.class);
        assertThat(first.get("reviewStar")).isInstanceOf(Number.class);
        assertThat(first.get("updatedAt")).isInstanceOf(String.class);
    }

    /* ============================================================
     * 2) 내 리뷰 목록 (/api/me/reviews)
     *    - RepositoryImpl.findByMemberIdWithProduct 커버
     * ============================================================ */

    @Test @DisplayName("내 리뷰 - 기본 정렬(updatedAt,desc)")
    void me_defaultSort_updatedAtDesc() {
        String url = baseUrl + "/me/reviews?page=0&size=9";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        List<Map<String, Object>> c = parseContent(res);
        assertThat(c).hasSize(1);
        assertThat(asLong(c.get(0).get("reviewId"))).isEqualTo(review1.getId());
    }

    // (컨트롤러 경유 null 문자열 PathVariable 테스트는 서비스 단위 테스트로 이관)

    @Test @DisplayName("내 리뷰 - reviewStar ASC/DESC, updatedAt ASC/DESC")
    void me_sort_all() {
        // 정렬 검증 강화를 위해 리뷰어의 추가 리뷰 데이터를 만든다 (product2, 낮은 별점)
        long tsLocal = System.nanoTime();
        Order extraOrder = Order.builder()
                .member(reviewer)
                .orderDate(LocalDate.now().minusDays(3))
                .totalPrice(110000)
                .orderCode("ORD-" + tsLocal + "-ME3")
                .build();
        orderRepository.save(extraOrder);

        Review myReview2 = Review.builder()
                .member(reviewer)
                .product(product2)
                .order(extraOrder)
                .reviewStar(2.0)
                .comment("두번째 리뷰")
                .build();
        reviewRepository.save(myReview2);

        String base = baseUrl + "/me/reviews?page=0&size=9";

        // reviewStar asc: 2.0 -> 4.5
        ResponseEntity<String> res1 = restTemplate.exchange(base + "&sort=reviewStar&sort=asc", HttpMethod.GET, auth(reviewerToken), String.class);
        List<Map<String, Object>> c1 = parseContent(res1);
        assertThat(c1).hasSize(2);
        assertThat(((Number)c1.get(0).get("reviewStar")).doubleValue()).isEqualTo(2.0);
        assertThat(((Number)c1.get(1).get("reviewStar")).doubleValue()).isEqualTo(4.5);

        // reviewStar desc: 4.5 -> 2.0
        ResponseEntity<String> res2 = restTemplate.exchange(base + "&sort=reviewStar&sort=desc", HttpMethod.GET, auth(reviewerToken), String.class);
        List<Map<String, Object>> c2 = parseContent(res2);
        assertThat(c2).hasSize(2);
        assertThat(((Number)c2.get(0).get("reviewStar")).doubleValue()).isEqualTo(4.5);
        assertThat(((Number)c2.get(1).get("reviewStar")).doubleValue()).isEqualTo(2.0);

        // updatedAt asc: 먼저 생성된 review1 -> 나중 생성된 myReview2
        ResponseEntity<String> res3 = restTemplate.exchange(base + "&sort=updatedAt&sort=asc", HttpMethod.GET, auth(reviewerToken), String.class);
        List<Map<String, Object>> c3 = parseContent(res3);
        assertThat(c3).hasSize(2);
        assertThat(asLong(c3.get(0).get("reviewId"))).isEqualTo(review1.getId());
        assertThat(asLong(c3.get(1).get("reviewId"))).isEqualTo(myReview2.getId());

        // updatedAt desc: 나중 생성된 myReview2 -> 먼저 생성된 review1
        ResponseEntity<String> res4 = restTemplate.exchange(base + "&sort=updatedAt&sort=desc", HttpMethod.GET, auth(reviewerToken), String.class);
        List<Map<String, Object>> c4 = parseContent(res4);
        assertThat(c4).hasSize(2);
        assertThat(asLong(c4.get(0).get("reviewId"))).isEqualTo(myReview2.getId());
        assertThat(asLong(c4.get(1).get("reviewId"))).isEqualTo(review1.getId());

        // 정리: 추가한 리뷰/주문 삭제 (다른 테스트 격리에 영향 없도록)
        try { if (myReview2.getId() != null) reviewRepository.deleteById(myReview2.getId()); } catch (Exception ignored) {}
        try { if (extraOrder.getId() != null) orderRepository.deleteById(extraOrder.getId()); } catch (Exception ignored) {}
    }

    @Test @DisplayName("내 리뷰 - 미지원 정렬필드 → 400")
    void me_unsupportedSortProperty_400() {
        String url = baseUrl + "/me/reviews?page=0&size=9&sort=notExistsProperty&sort=asc";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("내 리뷰 - 정렬 방향 INVALID → 400")
    void me_invalidDirection_400() {
        String url = baseUrl + "/me/reviews?page=0&size=9&sort=updatedAt&sort=INVALID";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    

    @Test @DisplayName("내 리뷰 - totalElements=1")
    void me_paging_total_one() {
        String url = baseUrl + "/me/reviews?page=0&size=10";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        Map<String,Object> root = parseRoot(res);
        assertThat(((Number)root.get("totalElements")).longValue()).isEqualTo(1L);
        assertThat(((Number)root.get("totalPages")).intValue()).isEqualTo(1);
        assertThat(parseContent(res)).hasSize(1);
    }

    @Test @DisplayName("내 리뷰 - totalElements=0 (셀러 계정은 리뷰 없음)")
    void me_paging_total_zero() {
        String url = baseUrl + "/me/reviews?page=0&size=10";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        Map<String,Object> root = parseRoot(res);
        assertThat(((Number)root.get("totalElements")).longValue()).isEqualTo(0L);
        assertThat(((Number)root.get("totalPages")).intValue()).isEqualTo(0);
        assertThat(parseContent(res)).isEmpty();
    }

    @Test @DisplayName("내 리뷰 - 응답 필드 스키마 확인")
    void me_responseSchema_ok() {
        String url = baseUrl + "/me/reviews?page=0&size=9";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        Map<String, Object> row = parseContent(res).get(0);
        assertThat(row.get("productName")).isEqualTo(product1.getProductName());
        assertThat(row.get("thumbnailImageUrl")).isEqualTo(product1.getThumbnailImageUrl());
        assertThat(row.get("comment")).isInstanceOf(String.class);
        assertThat(row.get("reviewStar")).isInstanceOf(Number.class);
        assertThat(row.get("updatedAt")).isInstanceOf(String.class);
    }

    

    /* ============================================================
     * 3) 리뷰 생성/수정/삭제 + 폼 조회
     *    - Service의 validateXXX 전 케이스 커버
     * ============================================================ */

    // ---- 생성
    @Test @DisplayName("리뷰 생성 - 성공(201)")
    void review_create_201() {
        String url = baseUrl + "/orders/" + orderSuccessId + "/review";
        Map<String,Object> body = Map.of("comment", "아주 만족스러웠습니다!", "reviewStar", 5.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
    }

    @Test @DisplayName("리뷰 생성 - 주문 없음(404)")
    void review_create_orderNotFound_404() {
        Order temp = Order.builder()
                .member(reviewer)
                .orderDate(LocalDate.now().minusDays(1))
                .totalPrice(50000)
                .orderStatus(OrderStatus.SUCCESS)
                .orderCode("ORD-" + System.nanoTime() + "-TMP")
                .build();
        orderRepository.save(temp);
        Long deletedId = temp.getId();
        orderRepository.deleteById(deletedId);

        String url = baseUrl + "/orders/" + deletedId + "/review";
        Map<String,Object> body = Map.of("comment","올바른 테스트 코멘트입니다", "reviewStar", 4.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(404);
    }

    @Test @DisplayName("리뷰 생성 - 주문 미완료(400)")
    void review_create_orderNotCompleted_400() {
        String url = baseUrl + "/orders/" + orderPendingId + "/review";
        Map<String,Object> body = Map.of("comment","올바른 테스트 코멘트입니다", "reviewStar", 4.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
    }

    @Test @DisplayName("리뷰 생성 - 주문 아이템 없음(400)")
    void review_create_orderEmpty_400() {
        Order newEmptyOrder = Order.builder()
                .member(reviewer)
                .orderDate(LocalDate.now().minusDays(1))
                .totalPrice(70000)
                .orderStatus(OrderStatus.SUCCESS)
                .orderCode("ORD-" + System.nanoTime() + "-E2")
                .build();
        orderRepository.save(newEmptyOrder);

        String url = baseUrl + "/orders/" + newEmptyOrder.getId() + "/review";
        Map<String,Object> body = Map.of("comment","테스트", "reviewStar", 4.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("리뷰 생성 - 주문 아이템의 상품 미존재(404)")
    void review_create_orderProductNotFound_404() {
        Order orderBadProduct = Order.builder()
                .member(reviewer)
                .orderDate(LocalDate.now().minusDays(1))
                .totalPrice(80000)
                .orderStatus(OrderStatus.SUCCESS)
                .orderCode("ORD-" + System.nanoTime() + "-X2")
                .build();
        OrderItem oiBad = OrderItem.createOrderItem(
                -999999L, "없음", null, 0, null, null, 0, 0,
                LocalDate.now().plusDays(12), 1, 0);
        orderBadProduct.addOrderItem(oiBad);
        orderRepository.save(orderBadProduct);

        String url = baseUrl + "/orders/" + orderBadProduct.getId() + "/review";
        Map<String,Object> body = Map.of("comment","올바른 테스트 코멘트입니다", "reviewStar", 4.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(404);
    }

    @Test @DisplayName("리뷰 생성 - 타인 주문(403)")
    void review_create_accessDenied_403() {
        String url = baseUrl + "/orders/" + orderSuccessId + "/review";
        Map<String,Object> body = Map.of("comment","권한오류", "reviewStar", 4.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, authJson(sellerToken, body), String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(403);
    }

    @Test @DisplayName("리뷰 생성 - 이미 리뷰 작성(409)")
    void review_create_alreadyReviewed_409() {
        String url = baseUrl + "/orders/" + orderSuccessId + "/review";
        Map<String,Object> first = Map.of("comment","이미 작성 사전 생성용 코멘트", "reviewStar", 5.0);
        ResponseEntity<String> created = restTemplate.exchange(url, HttpMethod.POST, authJson(reviewerToken, first), String.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);

        Map<String,Object> dup = Map.of("comment","중복 생성 시도 코멘트", "reviewStar", 4.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, authJson(reviewerToken, dup), String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(409);
        Map<String,Object> err = parseRoot(res);
        if (!err.isEmpty()) {
            assertThat(err.get("errorCode")).isEqualTo("ALREADY_REVIEWED");
        }
    }

    @Test @DisplayName("리뷰 생성 - @Valid 실패(코멘트 짧음)")
    void review_create_valid_shortComment_400() {
        String url = baseUrl + "/orders/" + orderSuccessId + "/review";
        Map<String,Object> body = Map.of("comment","짧다", "reviewStar", 4.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("리뷰 생성 - @Valid 실패(코멘트 null)")
    void review_create_valid_nullComment_400() {
        String url = baseUrl + "/orders/" + orderSuccessId + "/review";
        Map<String,Object> body = new HashMap<>();
        body.put("comment", null);
        body.put("reviewStar", 4.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("리뷰 생성 - @Valid 실패(별점 null)")
    void review_create_valid_nullStar_400() {
        String url = baseUrl + "/orders/" + orderSuccessId + "/review";
        Map<String,Object> body = new HashMap<>();
        body.put("comment", "유효한 코멘트 내용입니다.");
        body.put("reviewStar", null);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("리뷰 생성 - @Valid 실패(별점 범위 밖)")
    void review_create_valid_starOutOfRange_400() {
        String url = baseUrl + "/orders/" + orderSuccessId + "/review";
        Map<String,Object> body = Map.of("comment","유효한 코멘트 내용입니다.", "reviewStar", 6.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    

    // ---- 수정
    @Test @DisplayName("리뷰 수정 - 성공(200)")
    void review_update_200() {
        String url = baseUrl + "/reviews/" + review1.getId();
        Map<String,Object> body = Map.of("comment","수정된 코멘트 충분히 길다", "reviewStar", 4.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.PUT, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test @DisplayName("리뷰 수정 - 본인 아님(403)")
    void review_update_accessDenied_403() {
        String url = baseUrl + "/reviews/" + review1.getId();
        Map<String,Object> body = Map.of("comment","수정된 코멘트 충분히 길다", "reviewStar", 3.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.PUT, authJson(sellerToken, body), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        Map<String,Object> err = parseRoot(res);
        assertThat(err.get("errorCode")).isEqualTo("ACCESS_DENIED");
    }

    @Test @DisplayName("리뷰 수정 - 리뷰없음(404)")
    void review_update_notFound_404() {
        String url = baseUrl + "/reviews/999999";
        Map<String,Object> body = Map.of("comment","수정된 코멘트 충분히 길다", "reviewStar", 3.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.PUT, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        Map<String,Object> err = parseRoot(res);
        assertThat(err.get("errorCode")).isEqualTo("REVIEW_NOT_FOUND");
    }

    @Test @DisplayName("리뷰 수정 - @Valid 실패(코멘트 짧음)")
    void review_update_valid_shortComment_400() {
        String url = baseUrl + "/reviews/" + review1.getId();
        Map<String,Object> body = Map.of("comment","짧다", "reviewStar", 3.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.PUT, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("리뷰 수정 - @Valid 실패(코멘트 null)")
    void review_update_valid_nullComment_400() {
        String url = baseUrl + "/reviews/" + review1.getId();
        Map<String,Object> body = new HashMap<>();
        body.put("comment", null);
        body.put("reviewStar", 3.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.PUT, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("리뷰 수정 - @Valid 실패(별점 null)")
    void review_update_valid_nullStar_400() {
        String url = baseUrl + "/reviews/" + review1.getId();
        Map<String,Object> body = new HashMap<>();
        body.put("comment", "수정 코멘트 충분히 긺.");
        body.put("reviewStar", null);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.PUT, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("리뷰 수정 - @Valid 실패(별점 범위 밖)")
    void review_update_valid_starOutOfRange_400() {
        String url = baseUrl + "/reviews/" + review1.getId();
        Map<String,Object> body = Map.of("comment","수정 코멘트 충분히 길다", "reviewStar", 0.0);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.PUT, authJson(reviewerToken, body), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    

    // ---- 삭제
    @Test @DisplayName("리뷰 삭제 - 성공(204)")
    void review_delete_204() {
        String url = baseUrl + "/reviews/" + review1.getId();
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.DELETE, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(204);
    }

    @Test @DisplayName("리뷰 삭제 - 본인 아님(403)")
    void review_delete_accessDenied_403() {
        String url = baseUrl + "/reviews/" + review2.getId();
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.DELETE, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        Map<String,Object> err = parseRoot(res);
        assertThat(err.get("errorCode")).isEqualTo("ACCESS_DENIED");
    }

    @Test @DisplayName("리뷰 삭제 - 리뷰없음(404)")
    void review_delete_notFound_404() {
        String url = baseUrl + "/reviews/999999";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.DELETE, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        Map<String,Object> err = parseRoot(res);
        assertThat(err.get("errorCode")).isEqualTo("REVIEW_NOT_FOUND");
    }

    

    // ---- 작성 폼/수정 폼
    @Test @DisplayName("리뷰 작성 폼 - 성공(200)")
    void review_form_create_ok() {
        String url = baseUrl + "/orders/" + orderSuccessId + "/review/form";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
    }

    @Test @DisplayName("리뷰 작성 폼 - 주문없음(404)")
    void review_form_create_orderNotFound_404() {
        String url = baseUrl + "/orders/999999/review/form";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        Map<String,Object> err = parseRoot(res);
        assertThat(err.get("errorCode")).isEqualTo("ORDER_NOT_FOUND");
    }

    @Test @DisplayName("리뷰 작성 폼 - 타인 주문(403)")
    void review_form_create_accessDenied_403() {
        String url = baseUrl + "/orders/" + orderSuccessId + "/review/form";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        Map<String,Object> err = parseRoot(res);
        assertThat(err.get("errorCode")).isEqualTo("ACCESS_DENIED");
    }

    @Test @DisplayName("리뷰 작성 폼 - 주문 미완료(400)")
    void review_form_create_orderNotCompleted_400() {
        String url = baseUrl + "/orders/" + orderPendingId + "/review/form";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        Map<String,Object> err = parseRoot(res);
        assertThat(err.get("errorCode")).isEqualTo("ORDER_NOT_COMPLETED");
    }

    @Test @DisplayName("리뷰 작성 폼 - 이미 리뷰 작성(409)")
    void review_form_create_alreadyReviewed_409() {
        Long reviewedOrderId = orderRepository.findByOrderCode(code1).orElseThrow().getId();
        String url = baseUrl + "/orders/" + reviewedOrderId + "/review/form";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("리뷰 작성 폼 - 주문 아이템 없음(400)")
    void review_form_create_orderEmpty_400() {
        String url = baseUrl + "/orders/" + orderEmptyId + "/review/form";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        Map<String,Object> err = parseRoot(res);
        assertThat(err.get("errorCode")).isEqualTo("ORDER_EMPTY");
    }

    @Test @DisplayName("리뷰 작성 폼 - 주문 내 상품 미존재(404)")
    void review_form_create_orderProductNotFound_404() {
        String url = baseUrl + "/orders/" + orderInvalidProductId + "/review/form";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        Map<String,Object> err = parseRoot(res);
        assertThat(err.get("errorCode")).isEqualTo("PRODUCT_NOT_FOUND");
    }

    

    @Test @DisplayName("리뷰 수정 폼 - 성공(200)")
    void review_form_update_ok() {
        String url = baseUrl + "/reviews/" + review1.getId() + "/form";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test @DisplayName("리뷰 수정 폼 - 리뷰없음(404)")
    void review_form_update_notFound_404() {
        String url = baseUrl + "/reviews/999999/form";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        Map<String,Object> err = parseRoot(res);
        assertThat(err.get("errorCode")).isEqualTo("REVIEW_NOT_FOUND");
    }

    @Test @DisplayName("리뷰 수정 폼 - 본인 아님(403)")
    void review_form_update_accessDenied_403() {
        String url = baseUrl + "/reviews/" + review1.getId() + "/form";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    

    /* ============================================================
     * 4) 페이징 파라미터 유효성(컨트롤러/스프링 검증)
     *    - 음수 page/size → 400 (스프링이 변환/검증 단계에서 튕김)
     * ============================================================ */

    @Test @DisplayName("관리자 상품별 리뷰 - page<0 → 400")
    void admin_negativePage_400() {
        String url = baseUrl + "/admin/products/" + product1.getId() + "/reviews?page=-1&size=10";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        // Controller 바인딩/검증 정책에 따라 400 또는 200(empty)이 될 수 있음.
        // 정책상 400으로 기대 (잘못된 요청)
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("관리자 상품별 리뷰 - size<=0 → 400")
    void admin_nonPositiveSize_400() {
        String url = baseUrl + "/admin/products/" + product1.getId() + "/reviews?page=0&size=0";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(sellerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("내 리뷰 - page<0 → 400")
    void me_negativePage_400() {
        String url = baseUrl + "/me/reviews?page=-1&size=10";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test @DisplayName("내 리뷰 - size<=0 → 400")
    void me_nonPositiveSize_400() {
        String url = baseUrl + "/me/reviews?page=0&size=0";
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, auth(reviewerToken), String.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }
}
