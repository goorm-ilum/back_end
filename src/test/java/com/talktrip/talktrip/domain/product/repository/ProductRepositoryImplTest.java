package com.talktrip.talktrip.domain.product.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.like.repository.LikeRepositoryImpl;
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
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDSLTestConfig.class)
@EnableJpaAuditing
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProductRepositoryImplTest {

    @Autowired
    private JPAQueryFactory queryFactory;

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
    private ProductOption productOption;
    private Review review;
    private Like like;

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

        productOption = ProductOption.builder()
                .product(product)
                .startDate(LocalDate.now().plusDays(1))
                .stock(10)
                .price(100000)
                .build();

        productOptionRepository.save(productOption);

        // Order 생성
        Order order = Order.builder()
                .member(member)
                .orderDate(LocalDate.now())
                .totalPrice(100000)
                .orderCode("TEST-ORDER-001")
                .build();
        
        orderRepository.save(order);

        review = Review.builder()
                .member(member)
                .product(product)
                .order(order)  // Order 연결
                .comment("좋은 여행이었습니다")
                .reviewStar(4.5)
                .build();

        reviewRepository.save(review);

        like = Like.builder()
                .memberId(member.getId())
                .productId(product.getId())
                .build();

        likeRepository.save(like);
    }

    @Test
    @DisplayName("키워드로 상품을 검색하고 평균 별점과 좋아요 여부를 포함하여 조회한다")
    void searchProductsWithAvgStarAndLike_Success() {
        // given
        String keyword = "제주도";
        String countryName = null;
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStarAndLike> result = productRepository.searchProductsWithAvgStarAndLike(
                keyword, countryName, tomorrow, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        ProductWithAvgStarAndLike productWithAvgStarAndLike = result.getContent().get(0);
        assertThat(productWithAvgStarAndLike.getProduct().getProductName()).isEqualTo("제주도 여행");
        assertThat(productWithAvgStarAndLike.getAvgStar()).isEqualTo(4.5);
        assertThat(productWithAvgStarAndLike.getIsLiked()).isTrue();
    }

    @Test
    @DisplayName("키워드가 없을 때 모든 상품을 조회한다")
    void searchProductsWithAvgStarAndLike_EmptyKeyword() {
        // given
        String keyword = "";
        String countryName = null;
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStarAndLike> result = productRepository.searchProductsWithAvgStarAndLike(
                keyword, countryName, tomorrow, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProduct().getProductName()).isEqualTo("제주도 여행");
    }

    @Test
    @DisplayName("국가명으로 필터링하여 상품을 조회한다")
    void searchProductsWithAvgStarAndLike_WithCountryFilter() {
        // given
        String keyword = "";
        String countryName = "대한민국";
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStarAndLike> result = productRepository.searchProductsWithAvgStarAndLike(
                keyword, countryName, tomorrow, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProduct().getCountry().getName()).isEqualTo("대한민국");
    }

    @Test
    @DisplayName("비로그인 사용자로 상품을 검색한다")
    void searchProductsWithAvgStarAndLike_NonLoggedInUser() {
        // given
        String keyword = "제주도";
        String countryName = null;
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Long memberId = null;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStarAndLike> result = productRepository.searchProductsWithAvgStarAndLike(
                keyword, countryName, tomorrow, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        ProductWithAvgStarAndLike productWithAvgStarAndLike = result.getContent().get(0);
        assertThat(productWithAvgStarAndLike.getProduct().getProductName()).isEqualTo("제주도 여행");
        assertThat(productWithAvgStarAndLike.getAvgStar()).isEqualTo(4.5);
        assertThat(productWithAvgStarAndLike.getIsLiked()).isFalse(); // 비로그인 사용자는 좋아요하지 않은 것으로 처리
    }

    @Test
    @DisplayName("좋아요하지 않은 상품의 좋아요 여부는 false를 반환한다")
    void searchProductsWithAvgStarAndLike_NotLiked() {
        // given
        // 좋아요하지 않은 새로운 상품 생성
        Product productWithoutLike = Product.builder()
                .member(member)
                .productName("서울 여행")
                .description("아름다운 서울 여행")
                .thumbnailImageUrl("https://example.com/seoul.jpg")
                .country(country)
                .build();

        productRepository.save(productWithoutLike);

        ProductOption optionWithoutLike = ProductOption.builder()
                .product(productWithoutLike)
                .startDate(LocalDate.now().plusDays(1))
                .stock(10)
                .price(100000)
                .build();

        productOptionRepository.save(optionWithoutLike);

        String keyword = "서울";
        String countryName = null;
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStarAndLike> result = productRepository.searchProductsWithAvgStarAndLike(
                keyword, countryName, tomorrow, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        ProductWithAvgStarAndLike productWithAvgStarAndLike = result.getContent().get(0);
        assertThat(productWithAvgStarAndLike.getProduct().getProductName()).isEqualTo("서울 여행");
        assertThat(productWithAvgStarAndLike.getIsLiked()).isFalse();
    }

    @Test
    @DisplayName("판매자 상품 목록을 조회한다")
    void findSellerProducts_Success() {
        // given
        String status = "ACTIVE";
        String keyword = "";
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findSellerProducts(member.getId(), status, keyword, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("제주도 여행");
        assertThat(result.getContent().get(0).getMember().getId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("리뷰가 없는 상품의 평균 별점은 0.0을 반환한다")
    void searchProductsWithAvgStarAndLike_NoReviews() {
        // given
        // 리뷰가 없는 새로운 상품 생성
        Product productWithoutReview = Product.builder()
                .member(member)
                .productName("부산 여행")
                .description("아름다운 부산 여행")
                .thumbnailImageUrl("https://example.com/busan.jpg")
                .country(country)
                .build();

        productRepository.save(productWithoutReview);

        ProductOption optionWithoutReview = ProductOption.builder()
                .product(productWithoutReview)
                .startDate(LocalDate.now().plusDays(1))
                .stock(10)
                .price(100000)
                .build();

        productOptionRepository.save(optionWithoutReview);

        String keyword = "부산";
        String countryName = null;
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStarAndLike> result = productRepository.searchProductsWithAvgStarAndLike(
                keyword, countryName, tomorrow, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        ProductWithAvgStarAndLike productWithAvgStarAndLike = result.getContent().get(0);
        assertThat(productWithAvgStarAndLike.getProduct().getProductName()).isEqualTo("부산 여행");
        assertThat(productWithAvgStarAndLike.getAvgStar()).isEqualTo(0.0);
        assertThat(productWithAvgStarAndLike.getIsLiked()).isFalse();
    }

    @Test
    @DisplayName("상품 ID 목록으로 상품 요약 정보를 조회한다")
    void findProductSummariesByIds_Success() {
        // given
        List<Long> productIds = List.of(product.getId());

        // when
        List<Product> result = productRepository.findProductSummariesByIds(productIds);

        // then
        assertThat(result).hasSize(1);
        Product foundProduct = result.get(0);
        assertThat(foundProduct.getProductName()).isEqualTo("제주도 여행");
    }

    @Test
    @DisplayName("상품 ID 목록으로 평균 별점과 좋아요 상태를 포함한 상품 정보를 조회한다")
    void findProductsWithAvgStarAndLikeByIds_Success() {
        // given
        List<Long> productIds = List.of(product.getId());
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStarAndLike> result = productRepository.findProductsWithAvgStarAndLikeByIds(productIds, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        ProductWithAvgStarAndLike productWithDetails = result.getContent().get(0);
        assertThat(productWithDetails.getProduct().getProductName()).isEqualTo("제주도 여행");
        assertThat(productWithDetails.getAvgStar()).isEqualTo(4.5);
        assertThat(productWithDetails.getIsLiked()).isTrue();
    }

    @Test
    @DisplayName("상품 ID 목록으로 조회 시 좋아요하지 않은 상품은 isLiked가 false를 반환한다")
    void findProductsWithAvgStarAndLikeByIds_NotLiked() {
        // given
        // 좋아요하지 않은 새로운 상품 생성
        Product productWithoutLike = Product.builder()
                .member(member)
                .productName("부산 여행")
                .description("아름다운 부산 여행")
                .thumbnailImageUrl("https://example.com/busan.jpg")
                .country(country)
                .build();

        productRepository.save(productWithoutLike);

        ProductOption optionWithoutLike = ProductOption.builder()
                .product(productWithoutLike)
                .startDate(LocalDate.now().plusDays(1))
                .stock(10)
                .price(100000)
                .build();

        productOptionRepository.save(optionWithoutLike);

        List<Long> productIds = List.of(productWithoutLike.getId());
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStarAndLike> result = productRepository.findProductsWithAvgStarAndLikeByIds(productIds, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        ProductWithAvgStarAndLike productWithDetails = result.getContent().get(0);
        assertThat(productWithDetails.getProduct().getProductName()).isEqualTo("부산 여행");
        assertThat(productWithDetails.getIsLiked()).isFalse();
    }

    @Test
    @DisplayName("상품 ID 목록으로 조회 시 빈 리스트를 전달하면 빈 페이지를 반환한다")
    void findProductsWithAvgStarAndLikeByIds_EmptyList() {
        // given
        List<Long> productIds = List.of();
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStarAndLike> result = productRepository.findProductsWithAvgStarAndLikeByIds(productIds, memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("상품 ID 목록으로 조회 시 null 리스트를 전달하면 빈 페이지를 반환한다")
    void findProductsWithAvgStarAndLikeByIds_NullList() {
        // given
        List<Long> productIds = null;
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStarAndLike> result = productRepository.findProductsWithAvgStarAndLikeByIds(productIds, memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("상품 ID 목록으로 조회 시 페이징이 올바르게 적용된다")
    void findProductsWithAvgStarAndLikeByIds_WithPaging() {
        // given
        // 두 번째 상품 생성
        Product secondProduct = Product.builder()
                .member(member)
                .productName("서울 여행")
                .description("아름다운 서울 여행")
                .thumbnailImageUrl("https://example.com/seoul.jpg")
                .country(country)
                .build();

        productRepository.save(secondProduct);

        ProductOption secondOption = ProductOption.builder()
                .product(secondProduct)
                .startDate(LocalDate.now().plusDays(1))
                .stock(10)
                .price(100000)
                .build();

        productOptionRepository.save(secondOption);

        List<Long> productIds = List.of(product.getId(), secondProduct.getId());
        Long memberId = member.getId();
        Pageable pageable = PageRequest.of(0, 1); // 페이지 크기 1

        // when
        Page<ProductWithAvgStarAndLike> result = productRepository.findProductsWithAvgStarAndLikeByIds(productIds, memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("상품 상세 정보와 평균 별점, 좋아요 상태를 성공적으로 조회한다")
    void findByIdWithDetailsAndAvgStarAndLike_Success() {
        // given
        Long productId = product.getId();
        Long memberId = member.getId();
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // when
        Optional<ProductWithAvgStarAndLike> result = productRepository.findByIdWithDetailsAndAvgStarAndLike(productId, tomorrow, memberId);

        // then
        assertThat(result).isPresent();
        ProductWithAvgStarAndLike productWithDetails = result.get();
        assertThat(productWithDetails.getProduct().getId()).isEqualTo(productId);
        assertThat(productWithDetails.getProduct().getProductName()).isEqualTo("제주도 여행");
        assertThat(productWithDetails.getAvgStar()).isEqualTo(4.5);
        assertThat(productWithDetails.getIsLiked()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 상세 정보 조회 시 빈 결과를 반환한다")
    void findByIdWithDetailsAndAvgStarAndLike_NotFound() {
        // given
        Long nonExistentProductId = 999L;
        Long memberId = member.getId();
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // when
        Optional<ProductWithAvgStarAndLike> result = productRepository.findByIdWithDetailsAndAvgStarAndLike(nonExistentProductId, tomorrow, memberId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("비로그인 사용자로 상품 상세 정보 조회 시 좋아요 상태가 false를 반환한다")
    void findByIdWithDetailsAndAvgStarAndLike_NonLoggedInUser() {
        // given
        Long productId = product.getId();
        Long nonLoggedInMemberId = null;
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // when
        Optional<ProductWithAvgStarAndLike> result = productRepository.findByIdWithDetailsAndAvgStarAndLike(productId, tomorrow, nonLoggedInMemberId);

        // then
        assertThat(result).isPresent();
        ProductWithAvgStarAndLike productWithDetails = result.get();
        assertThat(productWithDetails.getProduct().getId()).isEqualTo(productId);
        assertThat(productWithDetails.getIsLiked()).isFalse();
    }

    @Test
    @DisplayName("상품과 모든 연관 엔티티를 한 번에 조회한다")
    void findProductWithAllDetailsById_Success() {
        // given
        Long productId = product.getId();

        // when
        Product result = productRepository.findProductWithAllDetailsById(productId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(productId);
        assertThat(result.getProductName()).isEqualTo("제주도 여행");
        assertThat(result.getMember()).isNotNull();
        assertThat(result.getCountry()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 모든 연관 엔티티 조회 시 null을 반환한다")
    void findProductWithAllDetailsById_NotFound() {
        // given
        Long nonExistentProductId = 999L;

        // when
        Product result = productRepository.findProductWithAllDetailsById(nonExistentProductId);

        // then
        assertThat(result).isNull();
    }
}
