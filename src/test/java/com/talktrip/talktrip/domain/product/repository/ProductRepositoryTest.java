package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.global.config.QueryDSLTestConfig;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDSLTestConfig.class)
@EnableJpaAuditing
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CountryRepository countryRepository;

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
                .continent("아시아")
                .build();
        countryRepository.save(country);

        product = Product.builder()
                .member(member)
                .productName("제주도 여행")
                .description("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .country(country)
                .build();
        productRepository.save(product);
    }

    @Test
    @DisplayName("상품을 성공적으로 저장한다")
    void save_Success() {
        // given
        Product newProduct = Product.builder()
                .member(member)
                .productName("부산 여행")
                .description("아름다운 부산 여행")
                .thumbnailImageUrl("https://example.com/busan.jpg")
                .country(country)
                .build();

        // when
        Product savedProduct = productRepository.save(newProduct);

        // then
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getProductName()).isEqualTo("부산 여행");
        assertThat(savedProduct.getMember()).isEqualTo(member);
    }

    @Test
    @DisplayName("ID로 상품을 성공적으로 조회한다")
    void findById_Success() {
        // when
        Optional<Product> foundProduct = productRepository.findById(product.getId());

        // then
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getProductName()).isEqualTo("제주도 여행");
        assertThat(foundProduct.get().getMember()).isEqualTo(member);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 상품 조회 시 빈 Optional을 반환한다")
    void findById_NotFound() {
        // when
        Optional<Product> foundProduct = productRepository.findById(999L);

        // then
        assertThat(foundProduct).isEmpty();
    }

    @Test
    @DisplayName("모든 상품을 성공적으로 조회한다")
    void findAll_Success() {
        // given
        Product product2 = Product.builder()
                .member(member)
                .productName("부산 여행")
                .description("아름다운 부산 여행")
                .thumbnailImageUrl("https://example.com/busan.jpg")
                .country(country)
                .build();
        productRepository.save(product2);

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = productRepository.findAll(pageable);

        // then
        assertThat(productPage.getContent()).hasSize(2);
        assertThat(productPage.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("상품을 성공적으로 삭제한다")
    void delete_Success() {
        // when
        productRepository.delete(product);

        // then
        Optional<Product> deletedProduct = productRepository.findById(product.getId());
        assertThat(deletedProduct).isEmpty();
    }

    @Test
    @DisplayName("삭제된 상품을 포함하여 ID로 조회한다")
    void findByIdIncludingDeleted_Success() {
        // given
        product.markDeleted();

        // when
        Optional<Product> foundProduct = productRepository.findByIdIncludingDeleted(product.getId());

        // then
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getProductName()).isEqualTo("제주도 여행");
        assertThat(foundProduct.get().isDeleted()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 ID로 삭제된 상품 조회 시 빈 Optional을 반환한다")
    void findByIdIncludingDeleted_NotFound() {
        // when
        Optional<Product> foundProduct = productRepository.findByIdIncludingDeleted(999L);

        // then
        assertThat(foundProduct).isEmpty();
    }

    @Test
    @DisplayName("판매자 ID와 함께 상품을 조회한다")
    void findByIdAndMemberIdIncludingDeleted_Success() {
        // when
        Optional<Product> foundProduct = productRepository.findByIdAndMemberIdIncludingDeleted(
                product.getId(), member.getId());

        // then
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getProductName()).isEqualTo("제주도 여행");
        assertThat(foundProduct.get().getMember().getId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("다른 판매자의 상품 조회 시 빈 Optional을 반환한다")
    void findByIdAndMemberIdIncludingDeleted_DifferentSeller() {
        // given
        Member differentMember = Member.builder()
                .accountEmail("different@test.com")
                .name("다른판매자")
                .nickname("다른판매자")
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
        memberRepository.save(differentMember);

        // when
        Optional<Product> foundProduct = productRepository.findByIdAndMemberIdIncludingDeleted(
                product.getId(), differentMember.getId());

        // then
        assertThat(foundProduct).isEmpty();
    }

    @Test
    @DisplayName("상품 ID 목록으로 상품 요약 정보를 조회한다")
    void findProductSummariesByIds_Success() {
        // given
        Product product2 = Product.builder()
                .member(member)
                .productName("부산 여행")
                .description("아름다운 부산 여행")
                .thumbnailImageUrl("https://example.com/busan.jpg")
                .country(country)
                .build();
        productRepository.save(product2);

        List<Long> productIds = List.of(product.getId(), product2.getId());

        // when
        List<Product> result = productRepository.findProductSummariesByIds(productIds);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductName()).isEqualTo("제주도 여행");
        assertThat(result.get(1).getProductName()).isEqualTo("부산 여행");
    }

    @Test
    @DisplayName("상품이 존재하는지 확인한다")
    void existsById_Success() {
        // when
        boolean exists = productRepository.existsById(product.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 존재 여부 확인 시 false를 반환한다")
    void existsById_NotFound() {
        // when
        boolean exists = productRepository.existsById(999L);

        // then
        assertThat(exists).isFalse();
    }
}
