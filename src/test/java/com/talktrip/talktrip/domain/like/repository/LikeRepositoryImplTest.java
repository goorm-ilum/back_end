package com.talktrip.talktrip.domain.like.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.talktrip.talktrip.domain.like.dto.ProductWithAvgStar;
import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.order.repository.OrderRepository;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.repository.ProductOptionRepository;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.talktrip.talktrip.global.config.QueryDSLTestConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDSLTestConfig.class)
@EnableJpaAuditing
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LikeRepositoryImplTest {

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

    private LikeRepositoryImpl likeRepositoryImpl;
    private Product product1;
    private Product product2;
    private Member seller;
    private Member user;

    @BeforeEach
    void setUp() {
        likeRepositoryImpl = new LikeRepositoryImpl(queryFactory);

        // 판매자 생성
        seller = Member.builder()
                .accountEmail("seller@test.com")
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(seller);

        // 사용자 생성
        user = Member.builder()
                .accountEmail("user@test.com")
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(user);

        // 상품 생성
        product1 = Product.builder()
                .member(seller)
                .productName("제주도 여행")
                .description("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .deleted(false)
                .build();

        product2 = Product.builder()
                .member(seller)
                .productName("부산 여행")
                .description("바다가 아름다운 부산 여행")
                .thumbnailImageUrl("https://example.com/busan.jpg")
                .deleted(false)
                .build();

        productRepository.saveAll(List.of(product1, product2));

        // 상품 옵션 생성
        ProductOption option1 = ProductOption.builder()
                .product(product1)
                .price(10000)
                .discountPrice(8000)
                .build();

        ProductOption option2 = ProductOption.builder()
                .product(product2)
                .price(15000)
                .discountPrice(12000)
                .build();

        productOptionRepository.saveAll(List.of(option1, option2));

        // 주문 생성 (리뷰를 위해 필요)
        Order order1 = Order.builder()
                .member(user)
                .orderStatus(OrderStatus.SUCCESS)
                .orderCode("order-code-1")
                .build();

        Order order2 = Order.builder()
                .member(user)
                .orderStatus(OrderStatus.SUCCESS)
                .orderCode("order-code-2")
                .build();

        orderRepository.saveAll(List.of(order1, order2));

        // 리뷰 생성
        Review review1 = Review.builder()
                .product(product1)
                .member(user)
                .order(order1)
                .reviewStar(4.0)
                .build();

        Review review2 = Review.builder()
                .product(product2)
                .member(user)
                .order(order2)
                .reviewStar(3.0)
                .build();

        reviewRepository.saveAll(List.of(review1, review2));

        // 좋아요 생성
        Like like1 = Like.builder()
                .productId(product1.getId())
                .memberId(user.getId())
                .build();

        Like like2 = Like.builder()
                .productId(product2.getId())
                .memberId(user.getId())
                .build();

        likeRepository.saveAll(List.of(like1, like2));
    }



    @Test
    @DisplayName("QueryDSL 구현체가 정상적으로 좋아요한 상품 목록을 조회한다")
    void findLikedProductsWithAvgStar_ReturnsCorrectResults() {
        // given
        Long memberId = user.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStar> result = likeRepositoryImpl.findLikedProductsWithAvgStar(memberId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);

        // 첫 번째 상품 검증
        ProductWithAvgStar firstProduct = result.getContent().get(0);
        assertThat(firstProduct.getProduct().getId()).isNotNull();
        assertThat(firstProduct.getProduct().getProductName()).isIn("제주도 여행", "부산 여행");
        assertThat(firstProduct.getAvgStar()).isNotNull();
    }

    @Test
    @DisplayName("QueryDSL 구현체가 updatedAt 정렬을 올바르게 처리한다")
    void findLikedProductsWithAvgStar_WithUpdatedAtSort_ReturnsCorrectOrder() {
        // given
        Long memberId = user.getId();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));

        // when
        Page<ProductWithAvgStar> result = likeRepositoryImpl.findLikedProductsWithAvgStar(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getProduct().getId()).isEqualTo(product2.getId());
        assertThat(result.getContent().get(1).getProduct().getId()).isEqualTo(product1.getId());
    }

    @Test
    @DisplayName("QueryDSL 구현체가 productName 정렬을 올바르게 처리한다")
    void findLikedProductsWithAvgStar_WithProductNameSort_ReturnsCorrectOrder() {
        // given
        Long memberId = user.getId();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "productName"));

        // when
        Page<ProductWithAvgStar> result = likeRepositoryImpl.findLikedProductsWithAvgStar(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getProduct().getProductName()).isEqualTo("부산 여행");
        assertThat(result.getContent().get(1).getProduct().getProductName()).isEqualTo("제주도 여행");
    }

    @Test
    @DisplayName("QueryDSL 구현체가 알 수 없는 정렬 필드를 기본값으로 처리한다")
    void findLikedProductsWithAvgStar_WithUnknownSort_ReturnsDefaultOrder() {
        // given
        Long memberId = user.getId();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "unknownField"));

        // when
        Page<ProductWithAvgStar> result = likeRepositoryImpl.findLikedProductsWithAvgStar(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        // 기본값으로 updatedAt DESC 정렬되어야 함
        assertThat(result.getContent().get(0).getProduct().getId()).isEqualTo(product2.getId());
        assertThat(result.getContent().get(1).getProduct().getId()).isEqualTo(product1.getId());
    }

    @Test
    @DisplayName("QueryDSL 구현체가 정렬이 없는 경우 기본값으로 처리한다")
    void findLikedProductsWithAvgStar_WithNoSort_ReturnsDefaultOrder() {
        // given
        Long memberId = user.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStar> result = likeRepositoryImpl.findLikedProductsWithAvgStar(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        // 기본값으로 updatedAt DESC 정렬되어야 함
        assertThat(result.getContent().get(0).getProduct().getId()).isEqualTo(product2.getId());
        assertThat(result.getContent().get(1).getProduct().getId()).isEqualTo(product1.getId());
    }

    @Test
    @DisplayName("QueryDSL 구현체가 페이징을 올바르게 처리한다")
    void findLikedProductsWithAvgStar_WithPaging_ReturnsCorrectPage() {
        // given
        Long memberId = user.getId();
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "updatedAt")); // 페이지 크기 1

        // when
        Page<ProductWithAvgStar> result = likeRepositoryImpl.findLikedProductsWithAvgStar(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent().get(0).getProduct().getId()).isEqualTo(product2.getId()); // 첫 번째 상품
    }

    @Test
    @DisplayName("QueryDSL 구현체가 좋아요한 상품이 없는 경우 빈 결과를 반환한다")
    void findLikedProductsWithAvgStar_WhenNoLikes_ReturnsEmptyResult() {
        // given
        Long memberId = 999L; // 존재하지 않는 사용자
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStar> result = likeRepositoryImpl.findLikedProductsWithAvgStar(memberId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("QueryDSL 구현체가 삭제된 상품을 제외한다")
    void findLikedProductsWithAvgStar_ExcludesDeletedProducts() {
        // given
        Product deletedProduct = Product.builder()
                .member(seller)
                .productName("삭제된 상품")
                .description("삭제된 상품입니다")
                .thumbnailImageUrl("https://example.com/deleted.jpg")
                .deleted(true)
                .build();
        productRepository.save(deletedProduct);

        Like deletedProductLike = Like.builder()
                .productId(deletedProduct.getId())
                .memberId(user.getId())
                .build();
        likeRepository.save(deletedProductLike);

        Long memberId = user.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStar> result = likeRepositoryImpl.findLikedProductsWithAvgStar(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2); // 삭제된 상품 제외하고 2개만
        assertThat(result.getContent())
                .noneMatch(resultItem -> resultItem.getProduct().getId().equals(deletedProduct.getId()));
    }

    @Test
    @DisplayName("QueryDSL 구현체가 모든 필드를 올바르게 매핑한다")
    void findLikedProductsWithAvgStar_MapsAllFieldsCorrectly() {
        // given
        Long memberId = user.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ProductWithAvgStar> result = likeRepositoryImpl.findLikedProductsWithAvgStar(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);

        ProductWithAvgStar firstResult = result.getContent().get(0);
        Product product = firstResult.getProduct();
        assertThat(product.getId()).isNotNull();
        assertThat(product.getProductName()).isNotNull();
        assertThat(product.getDescription()).isNotNull();
        assertThat(product.getThumbnailImageUrl()).isNotNull();
        assertThat(firstResult.getAvgStar()).isGreaterThanOrEqualTo(0.0);
    }
}
