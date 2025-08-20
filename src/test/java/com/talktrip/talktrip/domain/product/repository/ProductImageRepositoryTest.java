package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({QuerydslConfig.class, ProductImageRepositoryTest.AuditingTestConfig.class})
class ProductImageRepositoryTest {

    @Autowired ProductImageRepository productImageRepository;
    @Autowired EntityManager em;

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingTestConfig { }

    private Member cachedSeller;

    private Member seller() {
        if (cachedSeller != null) return cachedSeller;
        Member m = Member.builder()
                .accountEmail(SELLER_EMAIL)   // 고정 상수여도 OK (동일 테스트 내 1회만 persist)
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
        em.persist(m);
        cachedSeller = m;
        return m;
    }

    private Product product(String name) {
        Product p = Product.builder()
                .member(seller())
                .productName(name)
                .description(DESC)
                .deleted(false)
                .build();
        em.persist(p);
        return p;
    }

    private void image(Product p, String Url) {
        ProductImage img = ProductImage.builder()
                .product(p)
                .imageUrl(Url)
                .sortOrder(0)
                .build();
        em.persist(img);
    }

    @Test
    @DisplayName("findAllByProduct: 해당 상품의 이미지 목록만 반환(정렬 보장X → 포함성 검사)")
    void findAllByProduct_basic() {
        Product p1 = product(PRODUCT_NAME_1);

        image(p1, IMAGE_URL_1);
        image(p1, IMAGE_URL_2);

        em.flush();
        em.clear();

        List<ProductImage> list = productImageRepository.findAllByProduct(p1);
        assertThat(list).hasSize(2);
        assertThat(list).extracting(ProductImage::getImageUrl)
                .containsExactlyInAnyOrder(IMAGE_URL_1, IMAGE_URL_2);
        assertThat(list).allMatch(img -> img.getProduct().getId().equals(p1.getId()));
    }

    @Test
    @DisplayName("findAllByProduct: 이미지가 없는 상품은 빈 리스트(경계값)")
    void findAllByProduct_empty() {
        Product p = product(PRODUCT_NAME_3);
        List<ProductImage> list = productImageRepository.findAllByProduct(p);
        assertThat(list).isEmpty();
    }
}
