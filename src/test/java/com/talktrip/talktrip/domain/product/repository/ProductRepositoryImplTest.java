package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.repository.OrderRepository;
import com.talktrip.talktrip.domain.product.dto.ProductWithAvgStarAndLike;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.global.config.QueryDSLTestConfig;
import com.talktrip.talktrip.global.entity.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(QueryDSLTestConfig.class)
@EnableJpaAuditing
@ActiveProfiles("test")
class ProductRepositoryImplTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductOptionRepository productOptionRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private TestEntityManager testEntityManager;

    private Member member;
    private Country country;
    private Product product;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .accountEmail("test@test.com")
                .name("테스트유저")
                .nickname("테스트유저")
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(member);

        country = Country.builder()
                .id(1L)
                .name("대한민국")
                .build();
        testEntityManager.persist(country);

        product = Product.builder()
                .member(member)
                .productName("제주도 여행")
                .description("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .country(country)
                .build();
        productRepository.save(product);

        productOptionRepository.save(ProductOption.builder()
                .product(product)
                .startDate(LocalDate.now().plusDays(1))
                .stock(10)
                .price(100000)
                .discountPrice(90000)
                .build());

        Order order = Order.builder()
                .member(member)
                .orderDate(LocalDate.now())
                .totalPrice(100000)
                .orderCode("TEST-ORDER-001")
                .build();
        orderRepository.save(order);

        reviewRepository.save(Review.builder()
                .member(member)
                .product(product)
                .order(order)
                .comment("좋은 여행이었습니다")
                .reviewStar(4.5)
                .build());

        likeRepository.save(Like.builder()
                .memberId(member.getId())
                .productId(product.getId())
                .build());
    }

    @Test
    @DisplayName("키워드로 상품을 검색하고 평균 별점과 좋아요 여부를 포함하여 조회한다")
    void searchProductsWithAvgStarAndLike_Success() {
        Page<ProductWithAvgStarAndLike> result = productRepository.searchProductsWithAvgStarAndLike(
                "제주도", null, member.getId(), PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        ProductWithAvgStarAndLike dto = result.getContent().get(0);
        assertThat(dto.getProduct().getProductName()).isEqualTo("제주도 여행");
        assertThat(dto.getAvgStar()).isEqualTo(4.5);
        assertThat(dto.getIsLiked()).isTrue();
    }

    @Test
    @DisplayName("키워드가 없을 때 모든 상품을 조회한다")
    void searchProductsWithAvgStarAndLike_EmptyKeyword() {
        Page<ProductWithAvgStarAndLike> result = productRepository.searchProductsWithAvgStarAndLike(
                "", null, member.getId(), PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProduct().getProductName()).isEqualTo("제주도 여행");
    }

    @Test
    @DisplayName("검색 - 미지원 정렬 필드 시 400 발생")
    void searchProductsWithAvgStarAndLike_UnsupportedSort_Throws400() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("notExists")));
        assertThatThrownBy(() ->
                productRepository.searchProductsWithAvgStarAndLike("제주", null, member.getId(), pageable))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Unsupported sort property");
    }

    @Test
    @DisplayName("국가명으로 필터링하여 상품을 조회한다")
    void searchProductsWithAvgStarAndLike_WithCountryFilter() {
        Page<ProductWithAvgStarAndLike> result = productRepository.searchProductsWithAvgStarAndLike(
                "", "대한민국", member.getId(), PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProduct().getCountry().getName()).isEqualTo("대한민국");
    }

    @Test
    @DisplayName("비로그인 사용자로 상품을 검색한다")
    void searchProductsWithAvgStarAndLike_NonLoggedInUser() {
        Page<ProductWithAvgStarAndLike> result = productRepository.searchProductsWithAvgStarAndLike(
                "제주도", null, null, PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        ProductWithAvgStarAndLike dto = result.getContent().get(0);
        assertThat(dto.getProduct().getProductName()).isEqualTo("제주도 여행");
        assertThat(dto.getAvgStar()).isEqualTo(4.5);
        assertThat(dto.getIsLiked()).isFalse();
    }

    @Test
    @DisplayName("좋아요하지 않은 상품의 좋아요 여부는 false를 반환한다")
    void searchProductsWithAvgStarAndLike_NotLiked() {
        Product p = Product.builder()
                .member(member)
                .productName("서울 여행")
                .description("아름다운 서울 여행")
                .thumbnailImageUrl("https://example.com/seoul.jpg")
                .country(country)
                .build();
        productRepository.save(p);
        productOptionRepository.save(ProductOption.builder()
                .product(p)
                .startDate(LocalDate.now().plusDays(1))
                .stock(10)
                .price(100000)
                .discountPrice(95000)
                .build());

        Page<ProductWithAvgStarAndLike> result = productRepository.searchProductsWithAvgStarAndLike(
                "서울", null, member.getId(), PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        ProductWithAvgStarAndLike dto = result.getContent().getFirst();
        assertThat(dto.getProduct().getProductName()).isEqualTo("서울 여행");
        assertThat(dto.getIsLiked()).isFalse();
    }

    @Test
    @DisplayName("리뷰가 없는 상품의 평균 별점은 0.0을 반환한다")
    void searchProductsWithAvgStarAndLike_NoReviews() {
        Product p = Product.builder()
                .member(member)
                .productName("부산 여행")
                .description("아름다운 부산 여행")
                .thumbnailImageUrl("https://example.com/busan.jpg")
                .country(country)
                .build();
        productRepository.save(p);
        productOptionRepository.save(ProductOption.builder()
                .product(p)
                .startDate(LocalDate.now().plusDays(1))
                .stock(10)
                .price(100000)
                .discountPrice(97000)
                .build());

        Page<ProductWithAvgStarAndLike> result = productRepository.searchProductsWithAvgStarAndLike(
                "부산", null, member.getId(), PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        ProductWithAvgStarAndLike dto = result.getContent().get(0);
        assertThat(dto.getProduct().getProductName()).isEqualTo("부산 여행");
        assertThat(dto.getAvgStar()).isEqualTo(0.0);
        assertThat(dto.getIsLiked()).isFalse();
    }

    @Test
    @DisplayName("상품 ID 목록으로 상품 요약 정보를 조회한다")
    void findProductSummariesByIds_Success() {
        List<Long> productIds = List.of(product.getId());
        List<Product> result = productRepository.findProductSummariesByIds(productIds);
        assertThat(result).hasSize(1);
        Product found = result.get(0);
        assertThat(found.getProductName()).isEqualTo("제주도 여행");
    }

    @Test
    @DisplayName("상품 ID 목록으로 평균 별점과 좋아요 상태를 포함한 상품 정보를 조회한다")
    void findProductsWithAvgStarAndLikeByIds_Success() {
        Page<ProductWithAvgStarAndLike> result =
                productRepository.findProductsWithAvgStarAndLikeByIds(List.of(product.getId()), member.getId(), PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        ProductWithAvgStarAndLike dto = result.getContent().get(0);
        assertThat(dto.getProduct().getProductName()).isEqualTo("제주도 여행");
        assertThat(dto.getAvgStar()).isEqualTo(4.5);
        assertThat(dto.getIsLiked()).isTrue();
    }

    @Test
    @DisplayName("상품 ID 목록으로 조회 시 좋아요하지 않은 상품은 isLiked가 false를 반환한다")
    void findProductsWithAvgStarAndLikeByIds_NotLiked() {
        Product p = Product.builder()
                .member(member)
                .productName("부산 여행")
                .description("아름다운 부산 여행")
                .thumbnailImageUrl("https://example.com/busan.jpg")
                .country(country)
                .build();
        productRepository.save(p);
        productOptionRepository.save(ProductOption.builder()
                .product(p)
                .startDate(LocalDate.now().plusDays(1))
                .stock(10)
                .price(100000)
                .discountPrice(95000)
                .build());

        Page<ProductWithAvgStarAndLike> result =
                productRepository.findProductsWithAvgStarAndLikeByIds(List.of(p.getId()), member.getId(), PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        ProductWithAvgStarAndLike dto = result.getContent().get(0);
        assertThat(dto.getProduct().getProductName()).isEqualTo("부산 여행");
        assertThat(dto.getIsLiked()).isFalse();
    }

    @Test
    @DisplayName("상품 ID 목록으로 조회 시 빈 리스트를 전달하면 빈 페이지를 반환한다")
    void findProductsWithAvgStarAndLikeByIds_EmptyList() {
        Page<ProductWithAvgStarAndLike> result =
                productRepository.findProductsWithAvgStarAndLikeByIds(List.of(), member.getId(), PageRequest.of(0, 10));
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("상품 ID 목록으로 조회 시 null 리스트를 전달하면 빈 페이지를 반환한다")
    void findProductsWithAvgStarAndLikeByIds_NullList() {
        Page<ProductWithAvgStarAndLike> result =
                productRepository.findProductsWithAvgStarAndLikeByIds(null, member.getId(), PageRequest.of(0, 10));
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("상품 ID 목록으로 조회 시 페이징이 올바르게 적용된다")
    void findProductsWithAvgStarAndLikeByIds_WithPaging() {
        Product second = Product.builder()
                .member(member)
                .productName("서울 여행")
                .description("아름다운 서울 여행")
                .thumbnailImageUrl("https://example.com/seoul.jpg")
                .country(country)
                .build();
        productRepository.save(second);
        productOptionRepository.save(ProductOption.builder()
                .product(second)
                .startDate(LocalDate.now().plusDays(1))
                .stock(10)
                .price(100000)
                .discountPrice(95000)
                .build());

        Page<ProductWithAvgStarAndLike> result =
                productRepository.findProductsWithAvgStarAndLikeByIds(List.of(product.getId(), second.getId()), member.getId(), PageRequest.of(0, 1));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("국가명 필터 분기 - 전체/blank/null 처리")
    void searchProductsWithAvgStarAndLike_CountryBranches() {
        Pageable pageable = PageRequest.of(0, 10);
        assertThat(productRepository.searchProductsWithAvgStarAndLike("", "전체", member.getId(), pageable)).isNotNull();
        assertThat(productRepository.searchProductsWithAvgStarAndLike("", "  ", member.getId(), pageable)).isNotNull();
        assertThat(productRepository.searchProductsWithAvgStarAndLike("", null, member.getId(), pageable)).isNotNull();
    }

    @Test
    @DisplayName("상품 상세 정보와 평균 별점, 좋아요 상태를 성공적으로 조회한다")
    void findByIdWithDetailsAndAvgStarAndLike_Success() {
        Optional<ProductWithAvgStarAndLike> result =
                productRepository.findByIdWithDetailsAndAvgStarAndLike(product.getId(), member.getId());
        assertThat(result).isPresent();
        ProductWithAvgStarAndLike dto = result.get();
        assertThat(dto.getProduct().getId()).isEqualTo(product.getId());
        assertThat(dto.getProduct().getProductName()).isEqualTo("제주도 여행");
        assertThat(dto.getAvgStar()).isEqualTo(4.5);
        assertThat(dto.getIsLiked()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 상세 정보 조회 시 빈 결과를 반환한다")
    void findByIdWithDetailsAndAvgStarAndLike_NotFound() {
        Optional<ProductWithAvgStarAndLike> result =
                productRepository.findByIdWithDetailsAndAvgStarAndLike(999L, member.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("비로그인 사용자로 상품 상세 정보 조회 시 좋아요 상태가 false를 반환한다")
    void findByIdWithDetailsAndAvgStarAndLike_NonLoggedInUser() {
        Optional<ProductWithAvgStarAndLike> result =
                productRepository.findByIdWithDetailsAndAvgStarAndLike(product.getId(), null);
        assertThat(result).isPresent();
        ProductWithAvgStarAndLike dto = result.get();
        assertThat(dto.getProduct().getId()).isEqualTo(product.getId());
        assertThat(dto.getIsLiked()).isFalse();
    }

    @Test
    @DisplayName("상품 상세 - 미래 재고가 없으면 빈 결과")
    void findByIdWithDetailsAndAvgStarAndLike_NoFutureStock() {
        Product past = Product.builder()
                .member(member)
                .productName("과거 상품")
                .description("past")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(past);
        productOptionRepository.save(ProductOption.builder()
                .product(past)
                .startDate(LocalDate.now().minusDays(3))
                .stock(10)
                .price(10000)
                .discountPrice(9000)
                .build());

        Optional<ProductWithAvgStarAndLike> result =
                productRepository.findByIdWithDetailsAndAvgStarAndLike(past.getId(), member.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상품과 모든 연관 엔티티를 한 번에 조회한다")
    void findProductWithAllDetailsById_Success() {
        Product result = productRepository.findProductWithAllDetailsById(product.getId());
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(product.getId());
        assertThat(result.getProductName()).isEqualTo("제주도 여행");
        assertThat(result.getMember()).isNotNull();
        assertThat(result.getCountry()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 모든 연관 엔티티 조회 시 null을 반환한다")
    void findProductWithAllDetailsById_NotFound() {
        Product result = productRepository.findProductWithAllDetailsById(999L);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("검색 정렬 - productName ASC/DESC")
    void searchProductsWithAvgStarAndLike_SortByProductName() {
        Product a = Product.builder()
                .member(member)
                .productName("A-투어")
                .description("descA")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(a);
        productOptionRepository.save(ProductOption.builder()
                .product(a)
                .startDate(LocalDate.now().plusDays(1))
                .stock(3)
                .price(10000)
                .discountPrice(9000)
                .build());

        Product z = Product.builder()
                .member(member)
                .productName("Z-투어")
                .description("descZ")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(z);
        productOptionRepository.save(ProductOption.builder()
                .product(z)
                .startDate(LocalDate.now().plusDays(1))
                .stock(3)
                .price(10000)
                .discountPrice(9000)
                .build());

        Page<ProductWithAvgStarAndLike> ascPage =
                productRepository.searchProductsWithAvgStarAndLike("", null, member.getId(),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.asc("productName"))));
        Page<ProductWithAvgStarAndLike> descPage =
                productRepository.searchProductsWithAvgStarAndLike("", null, member.getId(),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.desc("productName"))));

        assertThat(ascPage.getContent()).isNotEmpty();
        assertThat(descPage.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("검색 정렬 - discountPrice ASC/DESC")
    void searchProductsWithAvgStarAndLike_SortByDiscountPrice() {
        Product pLow = Product.builder()
                .member(member)
                .productName("저가 투어")
                .description("d1")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(pLow);
        productOptionRepository.save(ProductOption.builder()
                .product(pLow)
                .startDate(LocalDate.now().plusDays(1))
                .stock(2)
                .price(10000)
                .discountPrice(8000)
                .build());

        Product pHigh = Product.builder()
                .member(member)
                .productName("고가 투어")
                .description("d2")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(pHigh);
        productOptionRepository.save(ProductOption.builder()
                .product(pHigh)
                .startDate(LocalDate.now().plusDays(1))
                .stock(2)
                .price(20000)
                .discountPrice(15000)
                .build());

        Page<ProductWithAvgStarAndLike> ascPage =
                productRepository.searchProductsWithAvgStarAndLike("", null, member.getId(),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.asc("discountPrice"))));
        Page<ProductWithAvgStarAndLike> descPage =
                productRepository.searchProductsWithAvgStarAndLike("", null, member.getId(),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.desc("discountPrice"))));

        assertThat(ascPage.getContent()).isNotEmpty();
        assertThat(descPage.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("검색 정렬 - averageReviewStar ASC/DESC")
    void searchProductsWithAvgStarAndLike_SortByReviewStar() {
        Product p1 = Product.builder()
                .member(member)
                .productName("리뷰1")
                .description("d1")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(p1);
        productOptionRepository.save(ProductOption.builder()
                .product(p1)
                .startDate(LocalDate.now().plusDays(1))
                .stock(2)
                .price(10000)
                .discountPrice(9000)
                .build());
        Order o1 = Order.builder()
                .member(member)
                .orderDate(LocalDate.now())
                .totalPrice(10000)
                .orderCode("T-ORD-1")
                .build();
        orderRepository.save(o1);
        reviewRepository.save(Review.builder().member(member).product(p1).order(o1).reviewStar(5.0).comment("a").build());

        Product p2 = Product.builder()
                .member(member)
                .productName("리뷰2")
                .description("d2")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(p2);
        productOptionRepository.save(ProductOption.builder()
                .product(p2)
                .startDate(LocalDate.now().plusDays(1))
                .stock(2)
                .price(10000)
                .discountPrice(9000)
                .build());
        Order o2 = Order.builder()
                .member(member)
                .orderDate(LocalDate.now())
                .totalPrice(20000)
                .orderCode("T-ORD-2")
                .build();
        orderRepository.save(o2);
        reviewRepository.save(Review.builder().member(member).product(p2).order(o2).reviewStar(3.0).comment("b").build());

        Page<ProductWithAvgStarAndLike> ascPage =
                productRepository.searchProductsWithAvgStarAndLike("", null, member.getId(),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.asc("averageReviewStar"))));
        Page<ProductWithAvgStarAndLike> descPage =
                productRepository.searchProductsWithAvgStarAndLike("", null, member.getId(),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.desc("averageReviewStar"))));

        assertThat(ascPage.getContent()).isNotEmpty();
        assertThat(descPage.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("키워드 필터 - 제목만 매칭")
    void searchProductsWithAvgStarAndLike_TitleOnly() {
        Product onlyName = Product.builder()
                .member(member)
                .productName("타이틀키워드 스페셜")
                .description("no-match")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(onlyName);
        productOptionRepository.save(ProductOption.builder()
                .product(onlyName)
                .startDate(LocalDate.now().plusDays(1))
                .stock(1)
                .price(10000)
                .discountPrice(9000)
                .build());

        Page<ProductWithAvgStarAndLike> page =
                productRepository.searchProductsWithAvgStarAndLike("타이틀키워드", null, member.getId(), PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("키워드 필터 - 설명만 매칭")
    void searchProductsWithAvgStarAndLike_DescriptionOnly() {
        Product onlyDesc = Product.builder()
                .member(member)
                .productName("no-match")
                .description("설명키워드 포함")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(onlyDesc);
        productOptionRepository.save(ProductOption.builder()
                .product(onlyDesc)
                .startDate(LocalDate.now().plusDays(1))
                .stock(1)
                .price(10000)
                .discountPrice(9000)
                .build());

        Page<ProductWithAvgStarAndLike> page =
                productRepository.searchProductsWithAvgStarAndLike("설명키워드", null, member.getId(), PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("키워드 필터 - 국가명 매칭(OR)")
    void searchProductsWithAvgStarAndLike_CountryAny() {
        Page<ProductWithAvgStarAndLike> page =
                productRepository.searchProductsWithAvgStarAndLike("대한민국", null, member.getId(), PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("키워드 필터 - 해시태그 중복 카운트 충족")
    void searchProductsWithAvgStarAndLike_HashTagCounts() {
        Product taggy = Product.builder()
                .member(member)
                .productName("태그 상품")
                .description("d")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(taggy);
        productOptionRepository.save(ProductOption.builder()
                .product(taggy)
                .startDate(LocalDate.now().plusDays(1))
                .stock(1)
                .price(10000)
                .discountPrice(9000)
                .build());

        testEntityManager.persist(new com.talktrip.talktrip.domain.product.entity.HashTag(null, taggy, "일본 맛집"));
        testEntityManager.persist(new com.talktrip.talktrip.domain.product.entity.HashTag(null, taggy, "일본 여행"));
        testEntityManager.persist(new com.talktrip.talktrip.domain.product.entity.HashTag(null, taggy, "도시 투어"));
        testEntityManager.persist(new com.talktrip.talktrip.domain.product.entity.HashTag(null, taggy, "여행 후기"));

        Page<ProductWithAvgStarAndLike> page =
                productRepository.searchProductsWithAvgStarAndLike("일본 일본 도시 여행", null, member.getId(), PageRequest.of(0, 10));
        assertThat(page.getContent().stream().anyMatch(d -> d.getProduct().getId().equals(taggy.getId()))).isTrue();
    }

    @Test
    @DisplayName("검색 정렬 - updatedAt ASC/DESC")
    void searchProductsWithAvgStarAndLike_SortByUpdatedAt() {
        Page<ProductWithAvgStarAndLike> pAsc =
                productRepository.searchProductsWithAvgStarAndLike("", null, member.getId(),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.asc("updatedAt"))));
        Page<ProductWithAvgStarAndLike> pDesc =
                productRepository.searchProductsWithAvgStarAndLike("", null, member.getId(),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.desc("updatedAt"))));
        assertThat(pAsc).isNotNull();
        assertThat(pDesc).isNotNull();
    }

    @Test
    @DisplayName("키워드 null 처리(필터 미적용)")
    void searchProductsWithAvgStarAndLike_NullKeyword() {
        Page<ProductWithAvgStarAndLike> result =
                productRepository.searchProductsWithAvgStarAndLike(null, "전체", member.getId(), PageRequest.of(0, 10));
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("국가명 필터 - 앞뒤 공백 및 대소문자 무시")
    void searchProductsWithAvgStarAndLike_CountryTrimCaseInsensitive() {
        Page<ProductWithAvgStarAndLike> result =
                productRepository.searchProductsWithAvgStarAndLike("", "  대한민국 ", member.getId(), PageRequest.of(0, 10));
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("검색 결과가 없으면 total=0")
    void searchProductsWithAvgStarAndLike_NoResult() {
        Page<ProductWithAvgStarAndLike> result =
                productRepository.searchProductsWithAvgStarAndLike("매칭안됨키워드", null, member.getId(), PageRequest.of(0, 10));
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("판매자 상품 목록을 조회한다")
    void findSellerProducts_Success() {
        Page<Product> result = productRepository.findSellerProducts(member.getId(), "ACTIVE", "", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("제주도 여행");
        assertThat(result.getContent().get(0).getMember().getId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("판매자 상품 목록 - 미지원 정렬 필드 시 400 발생")
    void findSellerProducts_UnsupportedSort_Throws400() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("notExists")));
        assertThatThrownBy(() ->
                productRepository.findSellerProducts(member.getId(), "ACTIVE", "", pageable))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Unsupported sort property");
    }

    @Test
    @DisplayName("판매자 상품 목록 - updatedAt 정렬 ASC/DESC")
    void findSellerProducts_SortByUpdatedAt() {
        Page<Product> pAsc = productRepository.findSellerProducts(member.getId(), "ACTIVE", "",
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("updatedAt"))));
        Page<Product> pDesc = productRepository.findSellerProducts(member.getId(), "ACTIVE", "",
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("updatedAt"))));
        assertThat(pAsc).isNotNull();
        assertThat(pDesc).isNotNull();
    }

    @Test
    @DisplayName("판매자 상품 목록 - 키워드 필터 동작")
    void findSellerProducts_WithKeyword() {
        Page<Product> page =
                productRepository.findSellerProducts(member.getId(), "ACTIVE", "제주도", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(Product::getProductName).contains("제주도 여행");
    }

    @Test
    @DisplayName("판매자 상품 목록 - 키워드 불일치 시 빈 결과")
    void findSellerProducts_KeywordNoMatch() {
        Page<Product> page =
                productRepository.findSellerProducts(member.getId(), "ACTIVE", "없는키워드", PageRequest.of(0, 10));
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("판매자 상품 목록 - 미지원 상태값은 400")
    void findSellerProducts_InvalidStatus_Throws400() {
        assertThatThrownBy(() ->
                productRepository.findSellerProducts(member.getId(), "WRONG", "", PageRequest.of(0, 10)))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Unsupported status");
    }

    @Test
    @DisplayName("판매자 상품 목록 - productName 정렬 ASC/DESC")
    void findSellerProducts_SortByProductName() {
        Product pA = Product.builder()
                .member(member)
                .productName("가가 여행")
                .description("A")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(pA);
        productOptionRepository.save(ProductOption.builder()
                .product(pA)
                .startDate(LocalDate.now().plusDays(1))
                .stock(1)
                .price(10000)
                .discountPrice(9000)
                .build());

        Product pB = Product.builder()
                .member(member)
                .productName("나나 여행")
                .description("B")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(pB);
        productOptionRepository.save(ProductOption.builder()
                .product(pB)
                .startDate(LocalDate.now().plusDays(1))
                .stock(1)
                .price(10000)
                .discountPrice(9000)
                .build());

        Page<Product> ascPage = productRepository.findSellerProducts(member.getId(), "ACTIVE", "",
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("productName"))));
        assertThat(ascPage.getContent()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(ascPage.getContent().get(0).getProductName()).isIn("가가 여행", "제주도 여행");

        Page<Product> descPage = productRepository.findSellerProducts(member.getId(), "ACTIVE", "",
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("productName"))));
        assertThat(descPage.getContent()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(descPage.getContent().get(0).getProductName()).isIn("나나 여행", "제주도 여행");
    }

    @Test
    @DisplayName("판매자 상품 목록 - totalStock 정렬 ASC/DESC")
    void findSellerProducts_SortByTotalStock() {
        Product p2 = Product.builder()
                .member(member)
                .productName("부산 투어")
                .description("B")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(p2);
        productOptionRepository.save(ProductOption.builder()
                .product(p2)
                .startDate(LocalDate.now().plusDays(1))
                .stock(5)
                .price(10000)
                .discountPrice(9000)
                .build());

        Page<Product> ascPage = productRepository.findSellerProducts(member.getId(), "ACTIVE", "",
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("totalStock"))));
        assertThat(ascPage.getContent()).hasSizeGreaterThanOrEqualTo(2);

        Page<Product> descPage = productRepository.findSellerProducts(member.getId(), "ACTIVE", "",
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("totalStock"))));
        assertThat(descPage.getContent()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("판매자 상품 목록 - 상태 DELETED 필터")
    void findSellerProducts_StatusDeleted() {
        product.markDeleted();
        Page<Product> deletedOnly = productRepository.findSellerProducts(member.getId(), "DELETED", "", PageRequest.of(0, 10));
        assertThat(deletedOnly.getContent()).isNotEmpty();
        assertThat(deletedOnly.getContent().stream().allMatch(Product::isDeleted)).isTrue();
    }

    @Test
    @DisplayName("판매자 상품 목록 - 상태 ALL 필터")
    void findSellerProducts_StatusAll() {
        Product extra = Product.builder()
                .member(member)
                .productName("서울 투어")
                .description("C")
                .thumbnailImageUrl(null)
                .country(country)
                .build();
        productRepository.save(extra);
        productOptionRepository.save(ProductOption.builder()
                .product(extra)
                .startDate(LocalDate.now().plusDays(1))
                .stock(3)
                .price(10000)
                .discountPrice(9000)
                .build());
        extra.markDeleted();

        Page<Product> all = productRepository.findSellerProducts(member.getId(), "ALL", "", PageRequest.of(0, 10));
        assertThat(all.getContent()).isNotEmpty();
        assertThat(all.getContent().stream().anyMatch(Product::isDeleted)).isTrue();
        assertThat(all.getContent().stream().anyMatch(p -> !p.isDeleted())).isTrue();
    }

    @Test
    @DisplayName("판매자 상품 목록 - 상태 미지정(null)")
    void findSellerProducts_Null() {
        assertThatThrownBy(() ->
                productRepository.findSellerProducts(member.getId(), null, "", PageRequest.of(0, 10)))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Unsupported status");
    }

    @Test
    @DisplayName("판매자 상품 목록 - 상태 미지정(Blank)")
    void findSellerProducts_Blank() {
        assertThatThrownBy(() ->
                productRepository.findSellerProducts(member.getId(), "   ", "", PageRequest.of(0, 10)))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Unsupported status");
    }
}
