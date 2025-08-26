package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.member.entity.Member;
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
import com.talktrip.talktrip.domain.product.repository.ProductHashTagRepository;
import com.talktrip.talktrip.domain.product.repository.ProductImageRepository;
import com.talktrip.talktrip.domain.product.repository.ProductOptionRepository;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.repository.CountryRepository;
import com.talktrip.talktrip.global.s3.S3Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductHashTagRepository productHashTagRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @Mock
    private S3Uploader s3Uploader;

    @InjectMocks
    private AdminProductService adminProductService;

    private Member adminMember;
    private Member regularMember;
    private Product product;
    private Product deletedProduct;
    private Country country;
    private AdminProductCreateRequest createRequest;
    private AdminProductUpdateRequest updateRequest;
    private MockMultipartFile thumbnailImage;
    private MockMultipartFile detailImage;

    @BeforeEach
    void setUp() {
        adminMember = Member.builder()
                .Id(1L)
                .accountEmail("admin@test.com")
                .phoneNum("010-1234-5678")
                .name("관리자")
                .nickname("관리자")
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();

        regularMember = Member.builder()
                .Id(2L)
                .accountEmail("user@test.com")
                .phoneNum("010-9876-5432")
                .name("일반유저")
                .nickname("일반유저")
                .memberRole(MemberRole.U)
                .memberState(MemberState.A)
                .build();

        country = Country.builder()
                .id(1L)
                .name("대한민국")
                .continent("아시아")
                .build();

        product = Product.builder()
                .id(1L)
                .productName("제주도 여행")
                .description("아름다운 제주도 여행")
                .thumbnailImageUrl("https://example.com/jeju.jpg")
                .member(adminMember)
                .country(country)
                .deleted(false)
                .build();

        deletedProduct = Product.builder()
                .id(2L)
                .productName("삭제된 제주도 여행")
                .description("삭제된 제주도 여행")
                .thumbnailImageUrl("https://example.com/deleted-jeju.jpg")
                .member(adminMember)
                .country(country)
                .deleted(true)  // 삭제된 상태
                .build();

        createRequest = AdminProductCreateRequest.builder()
                .productName("제주도 여행")
                .description("아름다운 제주도 여행")
                .countryName("대한민국")
                .hashtags(List.of("제주도", "여행", "관광"))
                .options(List.of(
                        ProductOptionRequest.builder()
                                .optionName("기본 패키지")
                                .startDate(LocalDate.now().plusDays(1))
                                .stock(10)
                                .price(100000)
                                .discountPrice(90000)
                                .build()
                ))
                .build();

        updateRequest = AdminProductUpdateRequest.builder()
                .productName("수정된 제주도 여행")
                .description("수정된 제주도 여행 설명")
                .countryName("대한민국")
                .hashtags(List.of("제주도", "여행", "관광"))
                .options(List.of(
                        ProductOptionRequest.builder()
                                .optionName("기본 패키지")
                                .startDate(LocalDate.now().plusDays(1))
                                .stock(10)
                                .price(100000)
                                .discountPrice(90000)
                                .build()
                ))
                .build();

        thumbnailImage = new MockMultipartFile(
                "thumbnailImage",
                "thumbnail.jpg",
                "image/jpeg",
                "thumbnail content".getBytes()
        );

        detailImage = new MockMultipartFile(
                "detailImage",
                "detail.jpg",
                "image/jpeg",
                "detail content".getBytes()
        );
    }

    @Test
    @DisplayName("상품을 성공적으로 생성한다")
    void createProduct_Success() {
        // given
        Long memberId = adminMember.getId();
        List<MultipartFile> detailImages = List.of(detailImage);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(s3Uploader.upload(any(), eq("products/thumbnail"))).thenReturn("https://s3.com/thumbnail.jpg");
        when(s3Uploader.calculateHash(any())).thenReturn("thumbnail-hash");
        when(s3Uploader.upload(any(), eq("products/detail"))).thenReturn("https://s3.com/detail.jpg");
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // when
        adminProductService.createProduct(createRequest, memberId, thumbnailImage, detailImages);

        // then
        verify(productRepository).save(any(Product.class));
        verify(s3Uploader).upload(thumbnailImage, "products/thumbnail");
        verify(s3Uploader).upload(detailImage, "products/detail");
    }

    @Test
    @DisplayName("null 회원 ID로 상품 생성 시 예외가 발생한다")
    void createProduct_NullMemberId() {
        // when & then
        assertThatThrownBy(() -> adminProductService.createProduct(createRequest, null, thumbnailImage, List.<MultipartFile>of()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 상품 생성 시 예외가 발생한다")
    void createProduct_MemberNotFound() {
        // given
        Long nonExistentMemberId = 999L;

        when(memberRepository.findById(nonExistentMemberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminProductService.createProduct(createRequest, nonExistentMemberId, thumbnailImage, List.<MultipartFile>of()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("일반 사용자로 상품 생성 시 예외가 발생한다")
    void createProduct_RegularUser() {
        // given
        Long memberId = regularMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        // when & then
        assertThatThrownBy(() -> adminProductService.createProduct(createRequest, memberId, thumbnailImage, List.<MultipartFile>of()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("존재하지 않는 국가로 상품 생성 시 예외가 발생한다")
    void createProduct_CountryNotFound() {
        // given
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("존재하지않는국가")).thenReturn(Optional.empty());

        AdminProductCreateRequest invalidRequest = AdminProductCreateRequest.builder()
                .productName("제주도 여행")
                .description("아름다운 제주도 여행")
                .countryName("존재하지않는국가")
                .hashtags(List.of("제주도", "여행", "관광"))
                .options(List.of(
                        ProductOptionRequest.builder()
                                .optionName("기본 패키지")
                                .startDate(LocalDate.now().plusDays(1))
                                .stock(10)
                                .price(100000)
                                .discountPrice(90000)
                                .build()
                ))
                .build();

        // when & then
        assertThatThrownBy(() -> adminProductService.createProduct(invalidRequest, memberId, thumbnailImage, List.<MultipartFile>of()))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUNTRY_NOT_FOUND);
    }

    @Test
    @DisplayName("내 상품 목록을 성공적으로 조회한다")
    void getMyProducts_Success() {
        // given
        Long memberId = adminMember.getId();
        String keyword = "제주도";
        String status = "ACTIVE";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findSellerProducts(memberId, status, keyword, pageable)).thenReturn(productPage);

        // when
        Page<AdminProductSummaryResponse> result = adminProductService.getMyProducts(memberId, keyword, status, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productName()).isEqualTo("제주도 여행");
    }

    @Test
    @DisplayName("ALL 상태로 상품 목록을 조회한다")
    void getMyProducts_AllStatus() {
        // given
        Long memberId = adminMember.getId();
        String keyword = "제주도";
        String status = "ALL";
        Pageable pageable = PageRequest.of(0, 10);
        
        // 활성화된 상품(product)과 삭제된 상품을 모두 포함
        Page<Product> productPage = new PageImpl<>(List.of(product, deletedProduct), pageable, 2);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findSellerProducts(memberId, status, keyword, pageable)).thenReturn(productPage);

        // when
        Page<AdminProductSummaryResponse> result = adminProductService.getMyProducts(memberId, keyword, status, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).productName()).isEqualTo("제주도 여행");
        assertThat(result.getContent().get(1).productName()).isEqualTo("삭제된 제주도 여행");
    }

    @Test
    @DisplayName("DELETED 상태로 상품 목록을 조회한다")
    void getMyProducts_DeletedStatus() {
        // given
        Long memberId = adminMember.getId();
        String keyword = "제주도";
        String status = "DELETED";
        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> productPage = new PageImpl<>(List.of(deletedProduct), pageable, 1);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findSellerProducts(memberId, status, keyword, pageable)).thenReturn(productPage);

        // when
        Page<AdminProductSummaryResponse> result = adminProductService.getMyProducts(memberId, keyword, status, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productName()).isEqualTo("삭제된 제주도 여행");
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 상품 목록 조회 시 예외가 발생한다")
    void getMyProducts_MemberNotFound() {
        // given
        Long nonExistentMemberId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.findById(nonExistentMemberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminProductService.getMyProducts(nonExistentMemberId, "제주도", "ACTIVE", pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("null 회원 ID로 상품 목록 조회 시 예외가 발생한다")
    void getMyProducts_NullMemberId() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> adminProductService.getMyProducts(null, "제주도", "ACTIVE", pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("일반 사용자로 상품 목록 조회 시 예외가 발생한다")
    void getMyProducts_RegularUser() {
        // given
        Long memberId = regularMember.getId();
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        // when & then
        assertThatThrownBy(() -> adminProductService.getMyProducts(memberId, "제주도", "ACTIVE", pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("null 회원 ID로 상품 수정 폼 조회 시 예외가 발생한다")
    void getMyProductEditForm_NullMemberId() {
        // given
        Long productId = product.getId();

        // when & then
        assertThatThrownBy(() -> adminProductService.getMyProductEditForm(productId, null))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 상품 수정 폼 조회 시 예외가 발생한다")
    void getMyProductEditForm_MemberNotFound() {
        // given
        Long productId = product.getId();
        Long nonExistentMemberId = 999L;

        when(memberRepository.findById(nonExistentMemberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminProductService.getMyProductEditForm(productId, nonExistentMemberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("일반 사용자로 상품 수정 폼 조회 시 예외가 발생한다")
    void getMyProductEditForm_RegularUser() {
        // given
        Long productId = product.getId();
        Long memberId = regularMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        // when & then
        assertThatThrownBy(() -> adminProductService.getMyProductEditForm(productId, memberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("상품 수정 폼을 성공적으로 조회한다")
    void getMyProductEditForm_Success() {
        // given
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);

        // when
        AdminProductEditResponse result = adminProductService.getMyProductEditForm(productId, memberId);

        // then
        verify(memberRepository).findById(memberId);
        verify(productRepository).findProductWithAllDetailsById(productId);
        assertThat(result).isNotNull();
        assertThat(result.productName()).isEqualTo("제주도 여행");
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 수정 폼 조회 시 예외가 발생한다")
    void getMyProductEditForm_ProductNotFound() {
        // given
        Long nonExistentProductId = 999L;
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(nonExistentProductId)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> adminProductService.getMyProductEditForm(nonExistentProductId, memberId))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 상품으로 수정 폼 조회 시 예외가 발생한다")
    void getMyProductEditForm_AccessDenied() {
        // given
        Long productId = product.getId();
        Long otherMemberId = 999L;

        when(memberRepository.findById(otherMemberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);

        // when & then
        assertThatThrownBy(() -> adminProductService.getMyProductEditForm(productId, otherMemberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("null 회원 ID로 상품 수정 시 예외가 발생한다")
    void updateProduct_NullMemberId() {
        // given
        Long productId = product.getId();

        // when & then
        assertThatThrownBy(() -> adminProductService.updateProduct(productId, updateRequest, null, thumbnailImage, List.<MultipartFile>of(), List.of()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 상품 수정 시 예외가 발생한다")
    void updateProduct_MemberNotFound() {
        // given
        Long productId = product.getId();
        Long nonExistentMemberId = 999L;

        when(memberRepository.findById(nonExistentMemberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminProductService.updateProduct(productId, updateRequest, nonExistentMemberId, thumbnailImage, List.<MultipartFile>of(), List.of()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("일반 사용자로 상품 수정 시 예외가 발생한다")
    void updateProduct_RegularUser() {
        // given
        Long productId = product.getId();
        Long memberId = regularMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        // when & then
        assertThatThrownBy(() -> adminProductService.updateProduct(productId, updateRequest, memberId, thumbnailImage, List.<MultipartFile>of(), List.of()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("상품을 성공적으로 수정한다")
    void updateProduct_Success() {
        // given
        Long productId = product.getId();
        Long memberId = adminMember.getId();
        List<MultipartFile> detailImages = List.of(detailImage);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(s3Uploader.calculateHash(any())).thenReturn("new-thumbnail-hash");
        when(s3Uploader.upload(any(), eq("products/thumbnail"))).thenReturn("https://s3.com/new-thumbnail.jpg");
        when(s3Uploader.upload(any(), eq("products/detail"))).thenReturn("https://s3.com/new-detail.jpg");
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());
        doNothing().when(productHashTagRepository).deleteAllByProduct(product);
        doNothing().when(productOptionRepository).deleteAllByProduct(product);

        // when
        adminProductService.updateProduct(productId, updateRequest, memberId, thumbnailImage, detailImages, List.of());

        // then
        verify(memberRepository).findById(memberId);
        verify(productRepository).findProductWithAllDetailsById(productId);
        verify(s3Uploader).upload(thumbnailImage, "products/thumbnail");
        verify(s3Uploader).upload(detailImage, "products/detail");
        verify(productImageRepository).findAllByProduct(product);
        verify(productHashTagRepository).deleteAllByProduct(product);
        verify(productOptionRepository).deleteAllByProduct(product);
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 수정 시 예외가 발생한다")
    void updateProduct_ProductNotFound() {
        // given
        Long nonExistentProductId = 999L;
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(nonExistentProductId)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> adminProductService.updateProduct(nonExistentProductId, updateRequest, memberId, thumbnailImage, List.<MultipartFile>of(), List.of()))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 상품으로 수정 시 예외가 발생한다")
    void updateProduct_AccessDenied() {
        // given
        Long productId = product.getId();
        Long otherMemberId = 999L;

        when(memberRepository.findById(otherMemberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminProductService.updateProduct(productId, updateRequest, otherMemberId, thumbnailImage, List.<MultipartFile>of(), List.of()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("null 회원 ID로 상품 삭제 시 예외가 발생한다")
    void deleteProduct_NullMemberId() {
        // given
        Long productId = product.getId();

        // when & then
        assertThatThrownBy(() -> adminProductService.deleteProduct(productId, null))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 상품 삭제 시 예외가 발생한다")
    void deleteProduct_MemberNotFound() {
        // given
        Long productId = product.getId();
        Long nonExistentMemberId = 999L;

        when(memberRepository.findById(nonExistentMemberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminProductService.deleteProduct(productId, nonExistentMemberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("일반 사용자로 상품 삭제 시 예외가 발생한다")
    void deleteProduct_RegularUser() {
        // given
        Long productId = product.getId();
        Long memberId = regularMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        // when & then
        assertThatThrownBy(() -> adminProductService.deleteProduct(productId, memberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("상품을 성공적으로 삭제한다")
    void deleteProduct_Success() {
        // given
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);

        // when
        adminProductService.deleteProduct(productId, memberId);

        // then
        verify(memberRepository).findById(memberId);
        verify(productRepository).findProductWithAllDetailsById(productId);
        assertThat(product.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 삭제 시 예외가 발생한다")
    void deleteProduct_ProductNotFound() {
        // given
        Long nonExistentProductId = 999L;
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(nonExistentProductId)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> adminProductService.deleteProduct(nonExistentProductId, memberId))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 상품으로 삭제 시 예외가 발생한다")
    void deleteProduct_AccessDenied() {
        // given
        Long productId = product.getId();
        Long otherMemberId = 999L;

        when(memberRepository.findById(otherMemberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminProductService.deleteProduct(productId, otherMemberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("null 회원 ID로 상품 복구 시 예외가 발생한다")
    void restoreProduct_NullMemberId() {
        // given
        Long productId = product.getId();

        // when & then
        assertThatThrownBy(() -> adminProductService.restoreProduct(productId, null))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 상품 복구 시 예외가 발생한다")
    void restoreProduct_MemberNotFound() {
        // given
        Long productId = product.getId();
        Long nonExistentMemberId = 999L;

        when(memberRepository.findById(nonExistentMemberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminProductService.restoreProduct(productId, nonExistentMemberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("일반 사용자로 상품 복구 시 예외가 발생한다")
    void restoreProduct_RegularUser() {
        // given
        Long productId = product.getId();
        Long memberId = regularMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        // when & then
        assertThatThrownBy(() -> adminProductService.restoreProduct(productId, memberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("상품을 성공적으로 복구한다")
    void restoreProduct_Success() {
        // given
        Long productId = product.getId();
        Long memberId = adminMember.getId();
        product.markDeleted();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);

        // when
        adminProductService.restoreProduct(productId, memberId);

        // then
        verify(memberRepository).findById(memberId);
        verify(productRepository).findProductWithAllDetailsById(productId);
        assertThat(product.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 복구 시 예외가 발생한다")
    void restoreProduct_ProductNotFound() {
        // given
        Long nonExistentProductId = 999L;
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(nonExistentProductId)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> adminProductService.restoreProduct(nonExistentProductId, memberId))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 상품으로 복구 시 예외가 발생한다")
    void restoreProduct_AccessDenied() {
        // given
        Long productId = product.getId();
        Long otherMemberId = regularMember.getId();

        when(memberRepository.findById(otherMemberId)).thenReturn(Optional.of(regularMember));

        // when & then
        assertThatThrownBy(() -> adminProductService.restoreProduct(productId, otherMemberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }
}
