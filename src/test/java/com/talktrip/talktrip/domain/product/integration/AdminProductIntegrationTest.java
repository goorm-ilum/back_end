package com.talktrip.talktrip.domain.product.integration;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.Gender;
import com.talktrip.talktrip.domain.member.enums.MemberRole;
import com.talktrip.talktrip.domain.member.enums.MemberState;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductCreateRequest;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductUpdateRequest;
import com.talktrip.talktrip.domain.product.dto.request.ProductOptionRequest;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductEditResponse;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.repository.ProductImageRepository;
import com.talktrip.talktrip.domain.product.repository.ProductOptionRepository;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.product.service.AdminProductService;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.repository.CountryRepository;
import com.talktrip.talktrip.global.s3.S3Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
class AdminProductIntegrationTest {

    @Autowired
    private AdminProductService adminProductService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @MockitoBean
    private S3Uploader s3Uploader;

    private Member adminMember;
    private Member regularMember;
    private Country country1;
    private Country country2;
    private Product product1;
    private Product product2;
    private Product deletedProduct;
    private ProductOption option1;
    private ProductOption option2;
    private ProductImage image1;
    private ProductImage image2;

    @BeforeEach
    void setUp() {
        // S3 Mock 설정
        when(s3Uploader.upload(any(MultipartFile.class), anyString()))
                .thenReturn("https://test-s3-url.com/test-image.jpg");
        when(s3Uploader.calculateHash(any(MultipartFile.class)))
                .thenReturn("test-hash-value");
        doNothing().when(s3Uploader).deleteFile(anyString());

        // 테스트 데이터 생성
        createTestData();
    }

    private void createTestData() {
        // 회원 생성
        adminMember = Member.builder()
                .accountEmail("admin@test.com")
                .phoneNum("010-1234-5678")
                .name("관리자")
                .nickname("관리자")
                .gender(Gender.M)
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
        adminMember = memberRepository.save(adminMember);

        regularMember = Member.builder()
                .accountEmail("user@test.com")
                .phoneNum("010-8765-4321")
                .name("일반사용자")
                .nickname("일반사용자")
                .gender(Gender.F)
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();
        regularMember = memberRepository.save(regularMember);

        // 국가 생성
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

        // 상품 생성
        product1 = Product.builder()
                .productName("제주도 여행")
                .description("아름다운 제주도 여행")
                .thumbnailImageUrl("https://test-s3-url.com/jeju-thumbnail.jpg")
                .member(adminMember)
                .country(country1)
                .build();
        product1 = productRepository.save(product1);

        product2 = Product.builder()
                .productName("도쿄 여행")
                .description("일본 도쿄 여행")
                .thumbnailImageUrl("https://test-s3-url.com/tokyo-thumbnail.jpg")
                .member(adminMember)
                .country(country2)
                .build();
        product2 = productRepository.save(product2);

        deletedProduct = Product.builder()
                .productName("삭제된 상품")
                .description("삭제된 상품입니다")
                .thumbnailImageUrl("https://test-s3-url.com/deleted-thumbnail.jpg")
                .member(adminMember)
                .country(country1)
                .build();
        deletedProduct = productRepository.save(deletedProduct);
        // 실제로 삭제 처리
        adminProductService.deleteProduct(deletedProduct.getId(), adminMember.getId());

        // 상품 옵션 생성
        option1 = ProductOption.builder()
                .optionName("기본 패키지")
                .price(100000)
                .discountPrice(90000)
                .stock(10)
                .startDate(LocalDate.now().plusDays(1))
                .product(product1)
                .build();
        option1 = productOptionRepository.save(option1);

        option2 = ProductOption.builder()
                .optionName("프리미엄 패키지")
                .price(200000)
                .discountPrice(180000)
                .stock(5)
                .startDate(LocalDate.now().plusDays(2))
                .product(product1)
                .build();
        option2 = productOptionRepository.save(option2);

        // 상품 이미지 생성
        image1 = ProductImage.builder()
                .imageUrl("https://test-s3-url.com/jeju-detail1.jpg")
                .sortOrder(1)
                .product(product1)
                .build();
        image1 = productImageRepository.save(image1);

        image2 = ProductImage.builder()
                .imageUrl("https://test-s3-url.com/jeju-detail2.jpg")
                .sortOrder(2)
                .product(product1)
                .build();
        image2 = productImageRepository.save(image2);
        image2 = productImageRepository.save(image2);
    }

    @Test
    @DisplayName("관리자가 상품을 성공적으로 생성한다")
    void createProduct_Success() {
        // given
        List<ProductOptionRequest> options = List.of(
                ProductOptionRequest.builder()
                        .optionName("패키지1")
                        .price(100000)
                        .discountPrice(90000)
                        .stock(10)
                        .startDate(LocalDate.now().plusDays(1))
                        .build(),
                ProductOptionRequest.builder()
                        .optionName("패키지2")
                        .price(200000)
                        .discountPrice(180000)
                        .stock(5)
                        .startDate(LocalDate.now().plusDays(2))
                        .build()
        );

        AdminProductCreateRequest request = new AdminProductCreateRequest(
                "새로운 상품",
                "새로운 상품 설명",
                "대한민국",
                options,
                List.of("해시태그1", "해시태그2")
        );

        MockMultipartFile thumbnailImage = new MockMultipartFile(
                "thumbnailImage",
                "test-thumbnail.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        MockMultipartFile detailImage1 = new MockMultipartFile(
                "detailImages",
                "test-detail1.jpg",
                "image/jpeg",
                "test detail image 1".getBytes()
        );

        MockMultipartFile detailImage2 = new MockMultipartFile(
                "detailImages",
                "test-detail2.jpg",
                "image/jpeg",
                "test detail image 2".getBytes()
        );

        List<MultipartFile> detailImages = List.of(detailImage1, detailImage2);

        // when
        adminProductService.createProduct(request, adminMember.getId(), thumbnailImage, detailImages);

        // then
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> !p.isDeleted())
                .toList();
        assertThat(products).hasSize(3); // 기존 2개(삭제되지 않은) + 새로 생성된 1개

        Product newProduct = products.stream()
                .filter(p -> p.getProductName().equals("새로운 상품"))
                .findFirst()
                .orElseThrow();

        assertThat(newProduct.getProductName()).isEqualTo("새로운 상품");
        assertThat(newProduct.getDescription()).isEqualTo("새로운 상품 설명");
        assertThat(newProduct.getCountry().getName()).isEqualTo("대한민국");
        assertThat(newProduct.getMember().getId()).isEqualTo(adminMember.getId());
        assertThat(newProduct.getThumbnailImageUrl()).isEqualTo("https://test-s3-url.com/test-image.jpg");

        // 상품 옵션 확인
        List<ProductOption> options_result = productOptionRepository.findAll().stream()
                .filter(opt -> opt.getProduct().getId().equals(newProduct.getId()))
                .toList();
        assertThat(options_result).hasSize(2);
        assertThat(options_result.get(0).getOptionName()).isEqualTo("패키지1");
        assertThat(options_result.get(0).getPrice()).isEqualTo(100000);
        assertThat(options_result.get(1).getOptionName()).isEqualTo("패키지2");
        assertThat(options_result.get(1).getPrice()).isEqualTo(200000);

        // 상품 이미지 확인 - Product 엔티티에서 직접 조회
        Product savedProduct = productRepository.findById(newProduct.getId()).orElseThrow();
        assertThat(savedProduct.getImages()).hasSize(2);
        assertThat(savedProduct.getImages().get(0).getImageUrl()).isEqualTo("https://test-s3-url.com/test-image.jpg");
        assertThat(savedProduct.getImages().get(0).getSortOrder()).isEqualTo(0); // 첫 번째 이미지는 sortOrder가 0
        assertThat(savedProduct.getImages().get(1).getImageUrl()).isEqualTo("https://test-s3-url.com/test-image.jpg");
        assertThat(savedProduct.getImages().get(1).getSortOrder()).isEqualTo(1); // 두 번째 이미지는 sortOrder가 1
    }

    @Test
    @DisplayName("일반 사용자가 상품 생성 시 예외가 발생한다")
    void createProduct_RegularUser_ThrowsException() {
        // given
        List<ProductOptionRequest> options = List.of(
                ProductOptionRequest.builder()
                        .optionName("패키지1")
                        .price(100000)
                        .discountPrice(90000)
                        .stock(10)
                        .startDate(LocalDate.now().plusDays(1))
                        .build()
        );

        AdminProductCreateRequest request = new AdminProductCreateRequest(
                "새로운 상품",
                "새로운 상품 설명",
                "대한민국",
                options,
                List.of("해시태그1")
        );

        MockMultipartFile thumbnailImage = new MockMultipartFile(
                "thumbnailImage",
                "test-thumbnail.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> adminProductService.createProduct(request, regularMember.getId(), thumbnailImage, List.of()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("존재하지 않는 국가로 상품 생성 시 예외가 발생한다")
    void createProduct_InvalidCountry_ThrowsException() {
        // given
        List<ProductOptionRequest> options = List.of(
                ProductOptionRequest.builder()
                        .optionName("패키지1")
                        .price(100000)
                        .discountPrice(90000)
                        .stock(10)
                        .startDate(LocalDate.now().plusDays(1))
                        .build()
        );

        AdminProductCreateRequest request = new AdminProductCreateRequest(
                "새로운 상품",
                "새로운 상품 설명",
                "존재하지 않는 국가",
                options,
                List.of("해시태그1")
        );

        MockMultipartFile thumbnailImage = new MockMultipartFile(
                "thumbnailImage",
                "test-thumbnail.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> adminProductService.createProduct(request, adminMember.getId(), thumbnailImage, List.of()))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUNTRY_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자가 자신의 상품 목록을 성공적으로 조회한다")
    void getMyProducts_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<AdminProductSummaryResponse> result = adminProductService.getMyProducts(
                adminMember.getId(), null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(2); // 삭제되지 않은 상품 2개
        assertThat(result.getTotalElements()).isEqualTo(2);

        List<String> productNames = result.getContent().stream()
                .map(AdminProductSummaryResponse::productName)
                .toList();
        assertThat(productNames).contains("제주도 여행", "도쿄 여행");
    }

    @Test
    @DisplayName("관리자가 키워드로 상품 목록을 필터링하여 조회한다")
    void getMyProducts_WithKeyword_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<AdminProductSummaryResponse> result = adminProductService.getMyProducts(
                adminMember.getId(), "제주도", null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productName()).isEqualTo("제주도 여행");
    }

    @Test
    @DisplayName("관리자가 삭제된 상품을 포함하여 조회한다")
    void getMyProducts_IncludeDeleted_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // 삭제된 상품이 실제로 존재하는지 확인
        Product deletedProductCheck = productRepository.findByIdIncludingDeleted(deletedProduct.getId()).orElseThrow();
        assertThat(deletedProductCheck.isDeleted()).isTrue();

        // when
        Page<AdminProductSummaryResponse> result = adminProductService.getMyProducts(
                adminMember.getId(), null, "DELETED", pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productName()).isEqualTo("삭제된 상품");
    }

    @Test
    @DisplayName("일반 사용자가 상품 목록 조회 시 예외가 발생한다")
    void getMyProducts_RegularUser_ThrowsException() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> adminProductService.getMyProducts(
                regularMember.getId(), null, null, pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("관리자가 상품 수정 폼을 성공적으로 조회한다")
    void getMyProductEditForm_Success() {
        // given
        Long productId = product1.getId();

        // when
        AdminProductEditResponse result = adminProductService.getMyProductEditForm(productId, adminMember.getId());

        // then
        assertThat(result.productName()).isEqualTo("제주도 여행");
        assertThat(result.description()).isEqualTo("아름다운 제주도 여행");
        assertThat(result.country()).isEqualTo("대한민국");
        assertThat(result.thumbnailImageUrl()).isEqualTo("https://test-s3-url.com/jeju-thumbnail.jpg");
        assertThat(result.options()).hasSize(2);
        assertThat(result.images()).hasSize(2);
    }

    @Test
    @DisplayName("다른 관리자의 상품 수정 폼 조회 시 예외가 발생한다")
    void getMyProductEditForm_OtherAdmin_ThrowsException() {
        // given
        Long productId = product1.getId();
        Member otherAdmin = Member.builder()
                .accountEmail("other-admin@test.com")
                .phoneNum("010-9999-9999")
                .name("다른관리자")
                .nickname("다른관리자")
                .gender(Gender.M)
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();
        otherAdmin = memberRepository.save(otherAdmin);
        Long otherAdminId = otherAdmin.getId();

        // when & then
        assertThatThrownBy(() -> adminProductService.getMyProductEditForm(productId, otherAdminId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("존재하지 않는 상품 수정 폼 조회 시 예외가 발생한다")
    void getMyProductEditForm_ProductNotFound_ThrowsException() {
        // given
        Long nonExistentProductId = 999L;

        // when & then
        assertThatThrownBy(() -> adminProductService.getMyProductEditForm(nonExistentProductId, adminMember.getId()))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자가 상품을 성공적으로 수정한다")
    void updateProduct_Success() {
        // given
        Long productId = product1.getId();
        
        List<ProductOptionRequest> options = List.of(
                ProductOptionRequest.builder()
                        .optionName("수정된 패키지1")
                        .price(150000)
                        .discountPrice(135000)
                        .stock(15)
                        .startDate(LocalDate.now().plusDays(3))
                        .build(),
                ProductOptionRequest.builder()
                        .optionName("수정된 패키지2")
                        .price(250000)
                        .discountPrice(225000)
                        .stock(8)
                        .startDate(LocalDate.now().plusDays(4))
                        .build()
        );

        AdminProductUpdateRequest request = new AdminProductUpdateRequest(
                "수정된 제주도 여행",
                "수정된 제주도 여행 설명",
                "일본",
                options,
                List.of("수정된 해시태그1", "수정된 해시태그2"),
                "https://test-s3-url.com/jeju-thumbnail.jpg",
                List.of(image1.getId(), image2.getId())
        );

        MockMultipartFile newThumbnailImage = new MockMultipartFile(
                "thumbnailImage",
                "new-thumbnail.jpg",
                "image/jpeg",
                "new thumbnail content".getBytes()
        );

        MockMultipartFile newDetailImage = new MockMultipartFile(
                "detailImages",
                "new-detail.jpg",
                "image/jpeg",
                "new detail image content".getBytes()
        );

        List<MultipartFile> newDetailImages = List.of(newDetailImage);
        List<String> detailImageOrder = List.of("id:" + image1.getId(), "new:0");

        // when
        adminProductService.updateProduct(productId, request, adminMember.getId(), 
                newThumbnailImage, newDetailImages, detailImageOrder);

        // then
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getProductName()).isEqualTo("수정된 제주도 여행");
        assertThat(updatedProduct.getDescription()).isEqualTo("수정된 제주도 여행 설명");
        assertThat(updatedProduct.getCountry().getName()).isEqualTo("일본");

        // 상품 옵션 확인
        List<ProductOption> updatedOptions = productOptionRepository.findAll().stream()
                .filter(opt -> opt.getProduct().getId().equals(updatedProduct.getId()))
                .toList();
        assertThat(updatedOptions).hasSize(2);
        assertThat(updatedOptions.get(0).getOptionName()).isEqualTo("수정된 패키지1");
        assertThat(updatedOptions.get(0).getPrice()).isEqualTo(150000);
        assertThat(updatedOptions.get(1).getOptionName()).isEqualTo("수정된 패키지2");
        assertThat(updatedOptions.get(1).getPrice()).isEqualTo(250000);
    }

    @Test
    @DisplayName("일반 사용자가 상품 수정 시 예외가 발생한다")
    void updateProduct_RegularUser_ThrowsException() {
        // given
        Long productId = product1.getId();
        
        List<ProductOptionRequest> options = List.of(
                ProductOptionRequest.builder()
                        .optionName("패키지1")
                        .price(100000)
                        .discountPrice(90000)
                        .stock(10)
                        .startDate(LocalDate.now().plusDays(1))
                        .build()
        );

        AdminProductUpdateRequest request = new AdminProductUpdateRequest(
                "수정된 상품",
                "수정된 설명",
                "대한민국",
                options,
                List.of("해시태그1"),
                "https://test-s3-url.com/jeju-thumbnail.jpg",
                List.of()
        );

        MockMultipartFile thumbnailImage = new MockMultipartFile(
                "thumbnailImage",
                "test-thumbnail.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> adminProductService.updateProduct(productId, request, regularMember.getId(),
                thumbnailImage, List.of(), List.of()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("관리자가 상품을 성공적으로 삭제한다")
    void deleteProduct_Success() {
        // given
        Long productId = product1.getId();

        // when
        adminProductService.deleteProduct(productId, adminMember.getId());

        // then
        Product deletedProduct = productRepository.findByIdIncludingDeleted(productId).orElseThrow();
        assertThat(deletedProduct.isDeleted()).isTrue();
        assertThat(deletedProduct.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("일반 사용자가 상품 삭제 시 예외가 발생한다")
    void deleteProduct_RegularUser_ThrowsException() {
        // given
        Long productId = product1.getId();

        // when & then
        assertThatThrownBy(() -> adminProductService.deleteProduct(productId, regularMember.getId()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("존재하지 않는 상품 삭제 시 예외가 발생한다")
    void deleteProduct_ProductNotFound_ThrowsException() {
        // given
        Long nonExistentProductId = 999L;

        // when & then
        assertThatThrownBy(() -> adminProductService.deleteProduct(nonExistentProductId, adminMember.getId()))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 삭제된 상품을 다시 삭제 시 예외가 발생한다")
    void deleteProduct_AlreadyDeletedProduct_ThrowsException() {
        // given
        Long deletedProductId = deletedProduct.getId(); // 이미 삭제된 상품

        // when & then
        assertThatThrownBy(() -> adminProductService.deleteProduct(deletedProductId, adminMember.getId()))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자가 삭제된 상품을 성공적으로 복구한다")
    void restoreProduct_Success() {
        // given
        Long productId = deletedProduct.getId();

        // when
        adminProductService.restoreProduct(productId, adminMember.getId());

        // then
        Product restoredProduct = productRepository.findById(productId).orElseThrow();
        assertThat(restoredProduct.isDeleted()).isFalse();
        assertThat(restoredProduct.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("일반 사용자가 상품 복구 시 예외가 발생한다")
    void restoreProduct_RegularUser_ThrowsException() {
        // given
        Long productId = deletedProduct.getId();

        // when & then
        assertThatThrownBy(() -> adminProductService.restoreProduct(productId, regularMember.getId()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("존재하지 않는 상품 복구 시 예외가 발생한다")
    void restoreProduct_ProductNotFound_ThrowsException() {
        // given
        Long nonExistentProductId = 999L;

        // when & then
        assertThatThrownBy(() -> adminProductService.restoreProduct(nonExistentProductId, adminMember.getId()))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제되지 않은 상품 복구 시 예외가 발생한다")
    void restoreProduct_NotDeletedProduct_ThrowsException() {
        // given
        Long productId = product1.getId(); // 삭제되지 않은 상품

        // when & then
        assertThatThrownBy(() -> adminProductService.restoreProduct(productId, adminMember.getId()))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("페이징 처리가 올바르게 작동한다")
    void getMyProducts_Pagination_Success() {
        // given
        // 추가 상품들을 생성하여 페이징 테스트
        for (int i = 0; i < 15; i++) {
            Product product = Product.builder()
                    .productName("테스트 상품 " + (i + 1))
                    .description("테스트 상품 설명 " + (i + 1))
                    .thumbnailImageUrl("https://test-s3-url.com/test" + (i + 1) + ".jpg")
                    .member(adminMember)
                    .country(country1)
                    .build();
            productRepository.save(product);
        }

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<AdminProductSummaryResponse> firstPage = adminProductService.getMyProducts(
                adminMember.getId(), null, null, pageable);

        Pageable secondPageable = PageRequest.of(1, 10);
        Page<AdminProductSummaryResponse> secondPage = adminProductService.getMyProducts(
                adminMember.getId(), null, null, secondPageable);

        // then
        assertThat(firstPage.getContent()).hasSize(10);
        assertThat(firstPage.getTotalElements()).isEqualTo(17); // 기존 2개 + 새로 생성된 15개
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.isFirst()).isTrue();
        assertThat(firstPage.isLast()).isFalse();

        assertThat(secondPage.getContent()).hasSize(7);
        assertThat(secondPage.getTotalElements()).isEqualTo(17);
        assertThat(secondPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.isFirst()).isFalse();
        assertThat(secondPage.isLast()).isTrue();
    }

    @Test
    @DisplayName("상품 옵션과 이미지가 올바르게 연관관계를 가진다")
    void productOptionsAndImages_Relationship_Success() {
        // given
        Long productId = product1.getId();

        // when
        AdminProductEditResponse result = adminProductService.getMyProductEditForm(productId, adminMember.getId());

        // then
        assertThat(result.options()).hasSize(2);
        assertThat(result.options().get(0).optionName()).isEqualTo("기본 패키지");
        assertThat(result.options().get(0).price()).isEqualTo(100000);
        assertThat(result.options().get(1).optionName()).isEqualTo("프리미엄 패키지");
        assertThat(result.options().get(1).price()).isEqualTo(200000);

        assertThat(result.images()).hasSize(2);
        assertThat(result.images().get(0).imageUrl()).isEqualTo("https://test-s3-url.com/jeju-detail1.jpg");
        assertThat(result.images().get(1).imageUrl()).isEqualTo("https://test-s3-url.com/jeju-detail2.jpg");
    }
}
