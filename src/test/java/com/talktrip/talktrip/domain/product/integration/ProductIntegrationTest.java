package com.talktrip.talktrip.domain.product.integration;

import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.Gender;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.ProductWithAvgStarAndLike;
import com.talktrip.talktrip.domain.product.dto.response.ProductDetailResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.entity.OrderItem;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.order.repository.OrderItemRepository;
import com.talktrip.talktrip.domain.order.repository.OrderRepository;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.repository.ProductOptionRepository;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.product.service.ProductService;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
class ProductIntegrationTest {

    @Autowired
    private ProductService productService;

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

    @BeforeEach
    void setUp() {
        // 테스트 데이터 생성
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
                .accountEmail("test1@test.com")
                .name("테스트유저1")
                .nickname("테스터1")
                .gender(Gender.M)
                .birthday(LocalDate.of(1990, 1, 1))
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(member1);

        member2 = Member.builder()
                .accountEmail("test2@test.com")
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
                .build();
        productRepository.save(product1);

        product2 = Product.builder()
                .productName("서울 도시 여행")
                .description("서울의 다양한 관광지를 둘러보는 상품")
                .thumbnailImageUrl("https://example.com/seoul.jpg")
                .member(member1)
                .country(country1)
                .build();
        productRepository.save(product2);

        product3 = Product.builder()
                .productName("도쿄 여행")
                .description("일본 도쿄의 매력을 느껴보세요")
                .thumbnailImageUrl("https://example.com/tokyo.jpg")
                .member(member2)
                .country(country2)
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
    }

    @Test
    @DisplayName("상품 검색을 성공적으로 수행한다 - 키워드 검색")
    void searchProducts_Success_WithKeyword() {
        // given
        String keyword = "제주도";
        String countryName = "전체";
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductSummaryResponse> result = productService.searchProducts(keyword, countryName, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        ProductSummaryResponse product = result.getContent().get(0);
        assertThat(product.productName()).isEqualTo("제주도 여행");
        assertThat(product.productDescription()).contains("제주도");
    }

    @Test
    @DisplayName("상품 검색을 성공적으로 수행한다 - 국가별 검색")
    void searchProducts_Success_WithCountry() {
        // given
        String keyword = null;
        String countryName = "대한민국";
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductSummaryResponse> result = productService.searchProducts(keyword, countryName, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting("productName")
                .containsExactlyInAnyOrder("제주도 여행", "서울 도시 여행");
    }

    @Test
    @DisplayName("상품 검색을 성공적으로 수행한다 - 전체 국가 검색")
    void searchProducts_Success_AllCountries() {
        // given
        String keyword = null;
        String countryName = "전체";
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductSummaryResponse> result = productService.searchProducts(keyword, countryName, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).extracting("productName")
                .containsExactlyInAnyOrder("제주도 여행", "서울 도시 여행", "도쿄 여행");
    }

    @Test
    @DisplayName("상품 검색 시 존재하지 않는 국가로 검색하면 예외가 발생한다")
    void searchProducts_NonExistentCountry_ThrowsException() {
        // given
        String keyword = null;
        String countryName = "존재하지않는국가";
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.searchProducts(keyword, countryName, memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUNTRY_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 검색 시 존재하지 않는 회원으로 검색하면 예외가 발생한다")
    void searchProducts_NonExistentMember_ThrowsException() {
        // given
        String keyword = "제주도";
        String countryName = "전체";
        Long nonExistentMemberId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.searchProducts(keyword, countryName, nonExistentMemberId, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 상세 정보를 성공적으로 조회한다")
    void getProductDetail_Success() {
        // given
        Long productId = product1.getId();
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        ProductDetailResponse result = productService.getProductDetail(productId, memberId, pageable);

        // then
        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.productName()).isEqualTo("제주도 여행");
        assertThat(result.countryName()).isEqualTo("대한민국");
        assertThat(result.averageReviewStar()).isEqualTo(4.75); // (4.5 + 5.0) / 2
        assertThat(result.isLiked()).isFalse(); // member1은 product1을 좋아요하지 않음
    }

    @Test
    @DisplayName("상품 상세 조회 시 좋아요한 상품은 isLiked가 true를 반환한다")
    void getProductDetail_WithLike() {
        // given
        Long productId = product1.getId();
        Long memberId = member2.getId(); // member2는 product1을 좋아요함
        Pageable pageable = PageRequest.of(0, 10);

        // when
        ProductDetailResponse result = productService.getProductDetail(productId, memberId, pageable);

        // then
        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.isLiked()).isTrue();
    }

    @Test
    @DisplayName("상품 상세 조회 시 존재하지 않는 상품이면 예외가 발생한다")
    void getProductDetail_NonExistentProduct_ThrowsException() {
        // given
        Long nonExistentProductId = 999L;
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(nonExistentProductId, memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 상세 조회 시 존재하지 않는 회원이면 예외가 발생한다")
    void getProductDetail_NonExistentMember_ThrowsException() {
        // given
        Long productId = product1.getId();
        Long nonExistentMemberId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(productId, nonExistentMemberId, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("AI 상품 검색 시 null 쿼리로 예외가 발생한다")
    void aiSearchProducts_NullQuery_ThrowsException() {
        // given
        String query = null;
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.aiSearchProducts(query, memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("AI 상품 검색 시 빈 쿼리로 예외가 발생한다")
    void aiSearchProducts_EmptyQuery_ThrowsException() {
        // given
        String query = "";
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.aiSearchProducts(query, memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("AI 상품 검색 시 존재하지 않는 회원으로 예외가 발생한다")
    void aiSearchProducts_NonExistentMember_ThrowsException() {
        // given
        String query = "제주도 여행";
        Long nonExistentMemberId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.aiSearchProducts(query, nonExistentMemberId, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 검색 시 페이징이 올바르게 적용된다")
    void searchProducts_WithPaging() {
        // given
        String keyword = null;
        String countryName = "전체";
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 2); // 첫 번째 페이지, 크기 2

        // when
        Page<ProductSummaryResponse> result = productService.searchProducts(keyword, countryName, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isFalse();
    }

    @Test
    @DisplayName("상품 상세 조회 시 리뷰 페이징이 올바르게 적용된다")
    void getProductDetail_WithReviewPaging() {
        // given
        Long productId = product1.getId();
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 1); // 첫 번째 페이지, 크기 1

        // when
        ProductDetailResponse result = productService.getProductDetail(productId, memberId, pageable);

        // then
        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.reviews()).hasSize(1); // 페이지 크기가 1이므로 1개만 반환
        assertThat(result.averageReviewStar()).isEqualTo(4.75); // 전체 평균은 그대로
    }

    @Test
    @DisplayName("상품 검색 시 키워드가 없으면 모든 상품을 반환한다")
    void searchProducts_NoKeyword_ReturnsAllProducts() {
        // given
        String keyword = null;
        String countryName = "전체";
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductSummaryResponse> result = productService.searchProducts(keyword, countryName, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).extracting("productName")
                .containsExactlyInAnyOrder("제주도 여행", "서울 도시 여행", "도쿄 여행");
    }

    @Test
    @DisplayName("상품 검색 시 빈 키워드로 검색하면 모든 상품을 반환한다")
    void searchProducts_EmptyKeyword_ReturnsAllProducts() {
        // given
        String keyword = "";
        String countryName = "전체";
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductSummaryResponse> result = productService.searchProducts(keyword, countryName, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).extracting("productName")
                .containsExactlyInAnyOrder("제주도 여행", "서울 도시 여행", "도쿄 여행");
    }

    @Test
    @DisplayName("상품 검색 시 존재하지 않는 국가로 검색하면 예외가 발생한다")
    void searchProducts_InvalidCountry_ThrowsException() {
        // given
        String keyword = "제주도";
        String invalidCountryName = "존재하지 않는 국가";
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.searchProducts(keyword, invalidCountryName, memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUNTRY_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 검색 시 null memberId로 예외가 발생한다")
    void searchProducts_NullMemberId_ThrowsException() {
        // given
        String keyword = "제주도";
        String countryName = "전체";
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.searchProducts(keyword, countryName, null, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 상세 조회 시 null memberId로 예외가 발생한다")
    void getProductDetail_NullMemberId_ThrowsException() {
        // given
        Long productId = product1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(productId, null, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 상세 조회 시 null productId로 예외가 발생한다")
    void getProductDetail_NullProductId_ThrowsException() {
        // given
        Long memberId = member1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(null, memberId, pageable))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }
}
