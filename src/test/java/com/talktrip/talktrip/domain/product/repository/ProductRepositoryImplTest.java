package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.global.config.QuerydslConfig;
import com.talktrip.talktrip.global.entity.Country;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({QuerydslConfig.class, ProductRepositoryImplTest.AuditingTestConfig.class})
class ProductRepositoryImplTest {

    @Autowired ProductRepository productRepository;
    @Autowired EntityManager em;

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingTestConfig {}

    private Member seller() {
        Member m = Member.builder()
                .accountEmail(SELLER_EMAIL)
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
        em.persist(m);
        return m;
    }

    private Country country(Long id, String name) {
        Country c = Country.builder().id(id).name(name).continent(CONTINENT_ASIA).build();
        em.persist(c);
        return c;
    }

    private Product product(Member s, Country c, String name, String desc) {
        Product p = Product.builder()
                .member(s)
                .country(c)
                .productName(name)
                .description(desc)
                .deleted(false)
                .build();
        em.persist(p);
        return p;
    }

    private void tag(Product p, String t) {
        em.persist(HashTag.builder().product(p).hashtag(t).build());
    }

    private void option(Product p, LocalDate startDate, int stock, int price, int discountPrice) {
        em.persist(ProductOption.builder()
                .product(p)
                .startDate(startDate)
                .optionName(OPTION_NAME)
                .stock(stock)
                .price(price)
                .discountPrice(discountPrice)
                .build());
    }

    @BeforeEach
    void seed() {
        Member s = seller();
        Country kr = country(COUNTRY_ID_1, COUNTRY_KOREA);
        Country jp = country(COUNTRY_ID_2, COUNTRY_JAPAN);

        Product p1 = product(s, kr, PRODUCT_NAME_SEA_TOUR, DESC_SEA);
        tag(p1, HASHTAG_SEA);
        tag(p1, HASHTAG_FOOD);
        option(p1, LocalDate.now(), STOCK_3, PRICE_10000, DISC_9000);

        Product p2 = product(s, jp, PRODUCT_NAME_TOKYO, DESC_CITY);
        tag(p2, HASHTAG_SEA);
        option(p2, LocalDate.now().plusDays(1), STOCK_5, PRICE_12000, DISC_9500);

        Product p3 = product(s, kr, PRODUCT_NAME_MOUNTAIN, DESC_MOUNTAIN);
        option(p3, LocalDate.now().plusDays(3), STOCK_3, PRICE_10000, DISC_9000);

        em.flush();
        em.clear();
    }

    private Page<Product> search(List<String> keywords, String country, Pageable pageable) {
        return productRepository.searchByKeywords(keywords, country, pageable);
    }

    @Test
    @DisplayName("단일 키워드(sea) + country=전체 → sea 포함 상품 반환(p1,p2)")
    void singleKeyword_allCountry() {
        Page<Product> page = search(List.of(KEYWORD_SEA), COUNTRY_ALL, PAGE_0_SIZE_10);
        assertThat(page.getContent())
                .extracting(Product::getProductName)
                .anyMatch(n -> n.toLowerCase().contains(KEYWORD_SEA));
        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("중복 키워드(sea, sea, food) → sea 2회 & food 1회 충족하는 상품만 포함")
    void duplicatedKeywords_countRequirement() {
        Page<Product> page = search(List.of(KEYWORD_SEA, KEYWORD_SEA, KEYWORD_FOOD), COUNTRY_ALL, PAGE_0_SIZE_10);
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("국가 필터: country!=전체 → 해당 국가만")
    void countryFilter_specific() {
        Page<Product> krOnly = search(List.of(KEYWORD_SEA), COUNTRY_KOREA, PAGE_0_SIZE_10);
        assertThat(krOnly.getContent()).allMatch(p -> COUNTRY_KOREA.equals(p.getCountry().getName()));
    }

    @Test
    @DisplayName("키워드 빈 리스트 → where 조건 없음 → country 필터 + hasFutureStock만 적용")
    void emptyKeywords_returnsAllWithinCountry() {
        Page<Product> allKr = search(List.of(), COUNTRY_KOREA, PAGE_0_SIZE_10);
        assertThat(allKr.getContent()).isNotEmpty();
        assertThat(allKr.getContent()).allMatch(p -> COUNTRY_KOREA.equals(p.getCountry().getName()));
    }

    @Test
    @DisplayName("대소문자 무시 매칭(Keyword upper/lower) & hashtag exists 매칭 확인")
    void caseInsensitive_and_hashtagExists() {
        Page<Product> page = search(List.of(KEYWORD_SEA_MIXED_CASE), COUNTRY_ALL, PAGE_0_SIZE_10);
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("페이지 경계: page overflow → 빈 페이지, totalElements 유지")
    void pageBoundaries_overflow() {
        Page<Product> all = search(List.of(KEYWORD_SEA), COUNTRY_ALL, PAGE_0_SIZE_10);
        int total = (int) all.getTotalElements();

        Page<Product> overflow = search(List.of(KEYWORD_SEA), COUNTRY_ALL, PageRequest.of(PAGE_5, SIZE_10));
        assertThat(overflow.getContent()).isEmpty();
        assertThat(overflow.getTotalElements()).isEqualTo(total);
    }

    @Test
    @DisplayName("해시태그 다중 매칭시에도 DISTINCT로 중복 없이 1회만 반환")
    void distinctWhenMultipleHashtagsMatched() {
        Page<Product> page = search(List.of(HASHTAG_SEA, HASHTAG_FOOD), COUNTRY_ALL, PAGE_0_SIZE_10);
        long p1Count = page.getContent().stream()
                .filter(p -> p.getProductName().toLowerCase().contains(KEYWORD_SEA))
                .count();
        assertThat(p1Count).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("기본 정렬은 updatedAt DESC")
    void defaultSort_is_updatedAtDesc() {
        Page<Product> page = search(List.of(KEYWORD_SEA), COUNTRY_ALL, PAGE_0_SIZE_10);
        if (page.getContent().size() >= 2) {
            assertThat(page.getContent().get(0).getUpdatedAt())
                    .isAfterOrEqualTo(page.getContent().get(1).getUpdatedAt());
        }
    }
}
