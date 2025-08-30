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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CountryRepository countryRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductHashTagRepository productHashTagRepository;
    @Mock private ProductOptionRepository productOptionRepository;
    @Mock private S3Uploader s3Uploader;

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

        country = Country.builder().id(1L).name("대한민국").continent("아시아").build();

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
                .deleted(true)
                .build();

        createRequest = AdminProductCreateRequest.builder()
                .productName("제주도 여행")
                .description("아름다운 제주도 여행")
                .countryName("대한민국")
                .hashtags(List.of("제주도", "여행"))
                .options(List.of(ProductOptionRequest.builder()
                        .optionName("기본 패키지")
                        .startDate(LocalDate.now().plusDays(1))
                        .stock(10)
                        .price(100000)
                        .discountPrice(90000)
                        .build()))
                .build();

        updateRequest = AdminProductUpdateRequest.builder()
                .productName("수정된 제주도 여행")
                .description("수정된 제주도 여행 설명")
                .countryName("대한민국")
                .hashtags(List.of("제주도", "여행"))
                .options(List.of(ProductOptionRequest.builder()
                        .optionName("기본 패키지")
                        .startDate(LocalDate.now().plusDays(1))
                        .stock(10)
                        .price(100000)
                        .discountPrice(90000)
                        .build()))
                .build();

        thumbnailImage = new MockMultipartFile("thumbnailImage", "thumbnail.jpg", "image/jpeg", "thumbnail".getBytes());
        detailImage = new MockMultipartFile("detailImage", "detail.jpg", "image/jpeg", "detail".getBytes());
    }

    @Test
    @DisplayName("상품을 성공적으로 생성한다")
    void createProduct_Success() {
        Long memberId = adminMember.getId();
        List<MultipartFile> detailImages = List.of(detailImage);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(s3Uploader.upload(any(), eq("products/thumbnail"))).thenReturn("https://s3.com/thumbnail.jpg");
        when(s3Uploader.calculateHash(any())).thenReturn("thumbnail-hash");
        when(s3Uploader.upload(any(), eq("products/detail"))).thenReturn("https://s3.com/detail.jpg");
        when(productRepository.save(any(Product.class))).thenReturn(product);

        adminProductService.createProduct(createRequest, memberId, thumbnailImage, detailImages);

        verify(productRepository).save(any(Product.class));
        verify(s3Uploader).upload(thumbnailImage, "products/thumbnail");
        verify(s3Uploader).upload(detailImage, "products/detail");
    }

    @Test
    @DisplayName("썸네일/디테일 이미지 없이 생성해도 업로드는 호출되지 않는다")
    void createProduct_NoImages_NoUpload() {
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        adminProductService.createProduct(createRequest, memberId, null, List.of());

        verify(s3Uploader, never()).upload(any(), anyString());
        verify(s3Uploader, never()).calculateHash(any());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("null 회원 ID로 상품 생성 시 예외")
    void createProduct_NullMemberId() {
        assertThatThrownBy(() -> adminProductService.createProduct(createRequest, null, thumbnailImage, List.of()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 상품 생성 시 예외")
    void createProduct_MemberNotFound() {
        Long nonExistentMemberId = 999L;
        when(memberRepository.findById(nonExistentMemberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminProductService.createProduct(createRequest, nonExistentMemberId, thumbnailImage, List.of()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("일반 사용자로 상품 생성 시 예외")
    void createProduct_RegularUser() {
        Long memberId = regularMember.getId();
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> adminProductService.createProduct(createRequest, memberId, thumbnailImage, List.of()))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("존재하지 않는 국가로 상품 생성 시 예외")
    void createProduct_CountryNotFound() {
        Long memberId = adminMember.getId();
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("존재하지않는국가")).thenReturn(Optional.empty());

        AdminProductCreateRequest invalid = AdminProductCreateRequest.builder()
                .productName("제주도 여행")
                .description("아름다운 제주도 여행")
                .countryName("존재하지않는국가")
                .options(createRequest.options())
                .hashtags(createRequest.hashtags())
                .build();

        assertThatThrownBy(() -> adminProductService.createProduct(
                invalid,
                memberId,
                thumbnailImage,
                List.of()))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUNTRY_NOT_FOUND);
    }

    @Test
    @DisplayName("내 상품 목록을 성공적으로 조회한다")
    void getMyProducts_Success() {
        Long memberId = adminMember.getId();
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findSellerProducts(memberId, "ACTIVE", "제주도", pageable))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));

        Page<AdminProductSummaryResponse> page =
                adminProductService.getMyProducts(memberId, "제주도", "ACTIVE", pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().productName()).isEqualTo("제주도 여행");
    }

    @Test
    @DisplayName("일반 사용자가 내 상품 목록 조회 시 ACCESS_DENIED")
    void getMyProducts_AccessDenied_ForNonAdmin() {
        Long memberId = regularMember.getId();
        Pageable pageable = PageRequest.of(0, 10);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> adminProductService.getMyProducts(memberId, "", "ACTIVE", pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("수정 폼 조회 성공")
    void getMyProductEditForm_Success() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);

        AdminProductEditResponse res = adminProductService.getMyProductEditForm(productId, memberId);

        verify(productRepository).findProductWithAllDetailsById(productId);
        assertThat(res.productName()).isEqualTo("제주도 여행");
    }

    @Test
    @DisplayName("다른 사용자가 수정 폼 조회 시 ACCESS_DENIED")
    void getMyProductEditForm_NotOwner_AccessDenied() {
        Long productId = product.getId();
        Long memberId = regularMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> adminProductService.getMyProductEditForm(productId, memberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("상품 수정 성공(썸네일 교체 + 디테일 이미지 fallback)")
    void updateProduct_Success() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();
        List<MultipartFile> details = List.of(detailImage);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(s3Uploader.calculateHash(any())).thenReturn("hash-1");
        when(s3Uploader.upload(any(), any())).thenReturn("https://s3.com/new.jpg");
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());
        doNothing().when(productHashTagRepository).deleteAllByProduct(product);
        doNothing().when(productOptionRepository).deleteAllByProduct(product);

        adminProductService.updateProduct(productId, updateRequest, memberId, thumbnailImage, details, List.of());

        verify(productImageRepository).findAllByProduct(product);
        verify(productHashTagRepository).deleteAllByProduct(product);
        verify(productOptionRepository).deleteAllByProduct(product);
        verify(s3Uploader).deleteFile("https://example.com/jeju.jpg"); // 기존 썸네일 삭제
    }

    @Test
    @DisplayName("상품 수정 - 썸네일 삭제 플로우(신규X, 기존 해시X)")
    void updateProduct_DeleteThumbnail() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        AdminProductUpdateRequest req = AdminProductUpdateRequest.builder()
                .productName("수정명")
                .description("수정설명")
                .countryName("대한민국")
                .hashtags(updateRequest.hashtags())
                .options(updateRequest.options())
                .existingThumbnailHash(null)
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        adminProductService.updateProduct(productId, req, memberId, null, List.of(), List.of());

        verify(s3Uploader).deleteFile("https://example.com/jeju.jpg");
        assertThat(product.getThumbnailImageUrl()).isNull();
    }

    @Test
    @DisplayName("상품 수정 - 썸네일 유지(신규 해시==기존 해시)")
    void updateProduct_KeepThumbnail_WhenHashSame() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        product.updateThumbnailImage("https://example.com/jeju.jpg", "same-hash");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(s3Uploader.calculateHash(any())).thenReturn("same-hash");
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        adminProductService.updateProduct(productId, updateRequest, memberId, thumbnailImage, List.of(), List.of());

        verify(s3Uploader, never()).deleteFile(anyString());
        verify(s3Uploader, never()).upload(eq(thumbnailImage), eq("products/thumbnail"));
    }

    @Test
    @DisplayName("디테일 이미지 - 정렬 토큰 기반 처리(id:+new:) 성공")
    void updateProduct_DetailImages_WithOrder_Happy() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        ProductImage img10 = ProductImage.builder()
                .product(product)
                .imageUrl("https://s3.com/old10.jpg")
                .sortOrder(0)
                .build();
        ReflectionTestUtils.setField(img10, "id", 10L);

        ProductImage img11 = ProductImage.builder()
                .product(product)
                .imageUrl("https://s3.com/old11.jpg")
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(img11, "id", 11L);


        List<MultipartFile> newFiles = List.of(
                new MockMultipartFile("nf0", "n0.jpg", "image/jpeg", "x".getBytes()),
                new MockMultipartFile("nf1", "n1.jpg", "image/jpeg", "y".getBytes())
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of(img10, img11));
        when(s3Uploader.upload(any(), eq("products/detail"))).thenReturn("https://s3.com/newX.jpg");

        adminProductService.updateProduct(
                productId,
                updateRequest,
                memberId,
                null,
                newFiles,
                List.of("id:10", "new:0", "id:999", "new:1")
        );

        verify(s3Uploader).deleteFile("https://s3.com/old11.jpg");
        verify(productImageRepository).delete(img11);
        verify(s3Uploader, times(2)).upload(any(), eq("products/detail"));
    }

    @Test
    @DisplayName("디테일 이미지 - 정렬 토큰이 잘못되면 예외")
    void updateProduct_DetailImages_WithOrder_InvalidToken() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        assertThatThrownBy(() -> adminProductService.updateProduct(
                productId, updateRequest, memberId, null, List.of(), List.of("oops")))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("디테일 이미지 - new: 인덱스 중복 사용 시 예외")
    void updateProduct_DetailImages_WithOrder_DuplicateNewIndex() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        List<MultipartFile> newFiles = List.of(
                new MockMultipartFile("nf0", "n0.jpg", "image/jpeg", "x".getBytes())
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        assertThatThrownBy(() -> adminProductService.updateProduct(
                productId, updateRequest, memberId, null, newFiles, List.of("new:0", "new:0")))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("디테일 이미지 - new: 인덱스 범위 밖이면 예외")
    void updateProduct_DetailImages_WithOrder_IndexOutOfBounds() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        List<MultipartFile> newFiles = List.of(
                new MockMultipartFile("nf0", "n0.jpg", "image/jpeg", "x".getBytes())
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        assertThatThrownBy(() -> adminProductService.updateProduct(
                productId, updateRequest, memberId, null, newFiles, List.of("new:5")))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("디테일 이미지 - 빈 파일이면 예외")
    void updateProduct_DetailImages_WithOrder_EmptyFile() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        List<MultipartFile> newFiles = List.of(
                new MockMultipartFile("nf0", "n0.jpg", "image/jpeg", new byte[0])
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        assertThatThrownBy(() -> adminProductService.updateProduct(
                productId, updateRequest, memberId, null, newFiles, List.of("new:0")))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("삭제 성공")
    void deleteProduct_Success() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);

        adminProductService.deleteProduct(productId, memberId);

        verify(productRepository).findProductWithAllDetailsById(productId);
        assertThat(product.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("복구 성공")
    void restoreProduct_Success() {
        Long productId = deletedProduct.getId();
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(deletedProduct);

        adminProductService.restoreProduct(productId, memberId);

        assertThat(deletedProduct.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("수정 - 상품 미존재 시 PRODUCT_NOT_FOUND")
    void updateProduct_ProductNotFound() {
        Long productId = 999L;
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(null);

        assertThatThrownBy(() -> adminProductService.updateProduct(
                productId, updateRequest, memberId, thumbnailImage, List.of(), List.of()))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제 - 상품 미존재 시 PRODUCT_NOT_FOUND")
    void deleteProduct_ProductNotFound() {
        Long productId = 999L;
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(null);

        assertThatThrownBy(() -> adminProductService.deleteProduct(productId, memberId))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("복구 - 상품 미존재 시 PRODUCT_NOT_FOUND")
    void restoreProduct_ProductNotFound() {
        Long productId = 999L;
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(null);

        assertThatThrownBy(() -> adminProductService.restoreProduct(productId, memberId))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 삭제된 상품 삭제 시 PRODUCT_NOT_FOUND")
    void deleteProduct_AlreadyDeleted_Throws() {
        Long productId = deletedProduct.getId();
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(deletedProduct);

        assertThatThrownBy(() -> adminProductService.deleteProduct(productId, memberId))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제되지 않은 상품 복구 시 PRODUCT_NOT_FOUND")
    void restoreProduct_NotDeleted_Throws() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);

        assertThatThrownBy(() -> adminProductService.restoreProduct(productId, memberId))
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자가 삭제/복구 시 ACCESS_DENIED")
    void delete_restore_AccessDenied_NotOwner() {
        Long productId = product.getId();
        Long memberId = regularMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> adminProductService.deleteProduct(productId, memberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);

        assertThatThrownBy(() -> adminProductService.restoreProduct(productId, memberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    // 썸네일: null/empty → 업로드/삭제 둘 다 안 함 (processThumbnailImage, updateThumbnailImage no-op 분기)
    @Test
    @DisplayName("업데이트 - 썸네일 유지 (thumbnail=null, existingHash!=null, no-op)")
    void updateProduct_KeepExistingThumbnail_NoOp() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        // 기존 썸네일 URL/해시 설정
        ReflectionTestUtils.setField(product, "thumbnailImageUrl", "https://s3.com/old.jpg");
        ReflectionTestUtils.setField(product, "thumbnailImageHash", "old-hash");

        // updateRequest: 기존 해시 전달(유지 의미), 썸네일 파일은 null
        AdminProductUpdateRequest keepReq = AdminProductUpdateRequest.builder()
                .productName(updateRequest.productName())
                .description(updateRequest.description())
                .countryName(updateRequest.countryName())
                .hashtags(updateRequest.hashtags())
                .options(updateRequest.options())
                .existingThumbnailHash("old-hash")
                .existingDetailImageIds(List.of()) // fallback 사용
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        adminProductService.updateProduct(productId, keepReq, memberId, null, null, null);

        verify(s3Uploader, never()).deleteFile(anyString());
        verify(s3Uploader, never()).upload(any(), anyString());
    }

    // 썸네일: 삭제 경로 (thumbnail=null && existingHash=null) + 기존 URL 없음 → deleteFile 호출 안 됨
    @Test
    @DisplayName("업데이트 - 썸네일 삭제 요청(기존 URL 없음) → 삭제 호출 없음")
    void updateProduct_DeleteThumbnail_NoExistingUrl() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        // 기존 URL을 null로
        ReflectionTestUtils.setField(product, "thumbnailImageUrl", null);
        ReflectionTestUtils.setField(product, "thumbnailImageHash", null);

        AdminProductUpdateRequest deleteReq = AdminProductUpdateRequest.builder()
                .productName(updateRequest.productName())
                .description(updateRequest.description())
                .countryName(updateRequest.countryName())
                .hashtags(updateRequest.hashtags())
                .options(updateRequest.options())
                .existingThumbnailHash(null) // 삭제 의도
                .existingDetailImageIds(List.of())
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        adminProductService.updateProduct(productId, deleteReq, memberId, null, null, null);

        verify(s3Uploader, never()).deleteFile(anyString());
        verify(s3Uploader, never()).upload(any(), anyString());
    }

    // 썸네일: 새 파일 업로드(해시 다름) → 기존 URL 있으면 delete → upload
    @Test
    @DisplayName("업데이트 - 썸네일 교체(해시 다름) → 기존 삭제 + 업로드")
    void updateProduct_ReplaceThumbnail_WithNewFile_DifferentHash() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        ReflectionTestUtils.setField(product, "thumbnailImageUrl", "https://s3.com/old.jpg");
        ReflectionTestUtils.setField(product, "thumbnailImageHash", "old-hash");

        MockMultipartFile newThumb = new MockMultipartFile("thumbnailImage", "new.jpg", "image/jpeg", "new".getBytes());

        AdminProductUpdateRequest req = AdminProductUpdateRequest.builder()
                .productName(updateRequest.productName())
                .description(updateRequest.description())
                .countryName(updateRequest.countryName())
                .hashtags(updateRequest.hashtags())
                .options(updateRequest.options())
                .existingThumbnailHash("old-hash") // 유지 의사 표시지만, 새 파일이 있으므로 교체 분기
                .existingDetailImageIds(List.of())
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());
        when(s3Uploader.calculateHash(newThumb)).thenReturn("new-hash");
        when(s3Uploader.upload(eq(newThumb), eq("products/thumbnail"))).thenReturn("https://s3.com/new.jpg");

        adminProductService.updateProduct(productId, req, memberId, newThumb, null, null);

        verify(s3Uploader, times(1)).deleteFile("https://s3.com/old.jpg");
        verify(s3Uploader, times(1)).upload(eq(newThumb), eq("products/thumbnail"));
    }

    // 썸네일: 새 파일 업로드(해시 같음) → 아무 것도 안 함
    @Test
    @DisplayName("업데이트 - 썸네일 교체(해시 동일) → 업로드/삭제 없음")
    void updateProduct_ReplaceThumbnail_SameHash_NoOp() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        ReflectionTestUtils.setField(product, "thumbnailImageUrl", "https://s3.com/old.jpg");
        ReflectionTestUtils.setField(product, "thumbnailImageHash", "same-hash");

        MockMultipartFile newThumb = new MockMultipartFile("thumbnailImage", "same.jpg", "image/jpeg", "same".getBytes());

        AdminProductUpdateRequest req = AdminProductUpdateRequest.builder()
                .productName(updateRequest.productName())
                .description(updateRequest.description())
                .countryName(updateRequest.countryName())
                .hashtags(updateRequest.hashtags())
                .options(updateRequest.options())
                .existingThumbnailHash("same-hash")
                .existingDetailImageIds(List.of())
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());
        when(s3Uploader.calculateHash(newThumb)).thenReturn("same-hash");

        adminProductService.updateProduct(productId, req, memberId, newThumb, null, null);

        verify(s3Uploader, never()).deleteFile(anyString());
        verify(s3Uploader, never()).upload(any(), anyString());
    }

    // 생성: 썸네일 empty → 업로드 안 함 (processThumbnailImage empty 분기)
    @Test
    @DisplayName("생성 - 썸네일 empty면 업로드 안 함")
    void createProduct_NoThumbnail_Empty() {
        Long memberId = adminMember.getId();
        MockMultipartFile empty = new MockMultipartFile("thumbnailImage", "t.jpg", "image/jpeg", new byte[0]);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        adminProductService.createProduct(createRequest, memberId, empty, List.of());

        verify(s3Uploader, never()).upload(eq(empty), anyString());
    }

    // 생성: 상세이미지 null/empty → 업로드 안 함 (processDetailImages null/empty 분기)
    @Test
    @DisplayName("생성 - 상세이미지 null/empty면 업로드 안 함")
    void createProduct_NoDetailImages() {
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        adminProductService.createProduct(createRequest, memberId, thumbnailImage, null);
        adminProductService.createProduct(createRequest, memberId, thumbnailImage, List.of());

        verify(s3Uploader, atLeastOnce()).upload(eq(thumbnailImage), eq("products/thumbnail"));
        verify(s3Uploader, never()).upload(any(), eq("products/detail"));
    }

    // 업데이트: 상세이미지 fallback - 일부 유지 + 일부 삭제 + 새로 추가
    @Test
    @DisplayName("업데이트 - 상세이미지 fallback: 일부 유지/삭제 + 새 이미지 추가")
    void updateProduct_DetailImages_Fallback_KeepSome_AddNew() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        ProductImage img10 = ProductImage.builder()
                .product(product).imageUrl("https://s3.com/old10.jpg").sortOrder(0).build();
        ReflectionTestUtils.setField(img10, "id", 10L);
        ProductImage img11 = ProductImage.builder()
                .product(product).imageUrl("https://s3.com/old11.jpg").sortOrder(1).build();
        ReflectionTestUtils.setField(img11, "id", 11L);

        MockMultipartFile new1 = new MockMultipartFile("detail", "n1.jpg", "image/jpeg", "n1".getBytes());

        // 기존 이미지 목록: 첫 호출시 [10,11], 두번째 호출부터는 [10]만 반환(11은 삭제된 상태를 시뮬레이션)
        when(productImageRepository.findAllByProduct(product))
                .thenReturn(List.of(img10, img11))
                .thenReturn(List.of(img10));

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(s3Uploader.upload(eq(new1), eq("products/detail"))).thenReturn("https://s3.com/new1.jpg");

        // keepOrder: 10만 유지, 11은 삭제 대상
        AdminProductUpdateRequest req = AdminProductUpdateRequest.builder()
                .productName(updateRequest.productName())
                .description(updateRequest.description())
                .countryName(updateRequest.countryName())
                .hashtags(updateRequest.hashtags())
                .options(updateRequest.options())
                .existingThumbnailHash("keep")
                .existingDetailImageIds(List.of(10L))
                .build();

        adminProductService.updateProduct(productId, req, memberId, null, List.of(new1), List.of());

        verify(s3Uploader, times(1)).deleteFile("https://s3.com/old11.jpg");
        verify(productImageRepository, times(1)).delete(img11); // 삭제된 이미지 repo 삭제
        verify(s3Uploader, times(1)).upload(eq(new1), eq("products/detail"));
    }

    // 업데이트: 상세이미지 fallback - keepOrder에 존재하지 않는 id 포함( exist == null 분기 커버)
    @Test
    @DisplayName("업데이트 - 상세이미지 fallback: keepOrder에 없는 id는 무시 (exist==null)")
    void updateProduct_DetailImages_Fallback_KeepOrder_WithMissingId() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        ProductImage img10 = ProductImage.builder()
                .product(product).imageUrl("https://s3.com/old10.jpg").sortOrder(0).build();
        ReflectionTestUtils.setField(img10, "id", 10L);

        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of(img10));

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));

        // keepOrder에 10(존재)과 999(존재하지 않음)를 넣음 → 999는 exist==null로 스킵
        AdminProductUpdateRequest req = AdminProductUpdateRequest.builder()
                .productName(updateRequest.productName())
                .description(updateRequest.description())
                .countryName(updateRequest.countryName())
                .hashtags(updateRequest.hashtags())
                .options(updateRequest.options())
                .existingThumbnailHash("keep")
                .existingDetailImageIds(List.of(10L, 999L))
                .build();

        adminProductService.updateProduct(productId, req, memberId, null, null, null);

        // 스킵되므로 추가적인 업로드/삭제는 없음
        verify(s3Uploader, never()).upload(any(), anyString());
        verify(s3Uploader, never()).deleteFile(anyString());
    }

    // 업데이트: 상세이미지 order 모드 - 기존/신규 섞어서 순서대로 + 참조 안된 기존은 삭제
    @Test
    @DisplayName("업데이트 - 상세이미지 order 모드: id/new 토큰 섞어서 적용 + 미참조 삭제")
    void updateProduct_DetailImages_WithOrder_Mixed() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        ProductImage img10 = ProductImage.builder()
                .product(product).imageUrl("https://s3.com/old10.jpg").sortOrder(0).build();
        ReflectionTestUtils.setField(img10, "id", 10L);
        ProductImage img11 = ProductImage.builder()
                .product(product).imageUrl("https://s3.com/old11.jpg").sortOrder(1).build();
        ReflectionTestUtils.setField(img11, "id", 11L);

        MockMultipartFile new0 = new MockMultipartFile("detail", "n0.jpg", "image/jpeg", "n0".getBytes());

        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of(img10, img11));
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(s3Uploader.upload(eq(new0), eq("products/detail"))).thenReturn("https://s3.com/n0.jpg");

        // order: id:10, new:0  (id:11은 참조되지 않아 삭제 대상)
        adminProductService.updateProduct(
                productId,
                updateRequest,
                memberId,
                null,
                List.of(new0),
                List.of("id:10", "new:0")
        );

        verify(s3Uploader, times(1)).deleteFile("https://s3.com/old11.jpg");
        verify(productImageRepository, times(1)).delete(img11);
        verify(s3Uploader, times(1)).upload(eq(new0), eq("products/detail"));
    }

    // 업데이트: 상세이미지 order 모드 - 잘못된 토큰 → 예외
    @Test
    @DisplayName("업데이트 - 상세이미지 order 모드: 잘못된 토큰 → 예외")
    void updateProduct_DetailImages_WithOrder_InvalidToken_Throws() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        assertThatThrownBy(() -> adminProductService.updateProduct(
                productId, updateRequest, memberId, null, List.of(detailImage), List.of("oops:0")
        )).isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_UPLOAD_FAILED);
    }

    // 업데이트: 상세이미지 order 모드 - new:idx 범위 초과 → 예외
    @Test
    @DisplayName("업데이트 - 상세이미지 order 모드: new 인덱스 범위 초과 → 예외")
    void updateProduct_DetailImages_WithOrder_NewIndexOutOfRange_Throws() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        // detailImages는 1개인데 new:1을 요구 → out of range
        assertThatThrownBy(() -> adminProductService.updateProduct(
                productId, updateRequest, memberId, null, List.of(detailImage), List.of("new:1")
        )).isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_UPLOAD_FAILED);
    }

    // 업데이트: 상세이미지 order 모드 - new:0 두 번 사용 → 예외 (중복 사용)
    @Test
    @DisplayName("업데이트 - 상세이미지 order 모드: 같은 new 인덱스 두 번 사용 → 예외")
    void updateProduct_DetailImages_WithOrder_DuplicateNewIndex_Throws() {
        Long productId = product.getId();
        Long memberId = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        assertThatThrownBy(() -> adminProductService.updateProduct(
                productId, updateRequest, memberId, null, List.of(detailImage), List.of("new:0", "new:0")
        )).isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_UPLOAD_FAILED);
    }

    // 소유권 검증: 다른 회원이 접근 → ACCESS_DENIED
    @Test
    @DisplayName("소유권 검증 - 다른 회원이 수정 폼 요청 시 ACCESS_DENIED")
    void getMyProductEditForm_AccessDenied_WhenOwnerMismatch() {
        Long productId = product.getId();
        Long otherMemberId = regularMember.getId();

        when(memberRepository.findById(otherMemberId)).thenReturn(Optional.of(regularMember)); // 관리자가 아님
        // validateAdminMember에서 이미 ACCESS_DENIED가 나가므로 아래 스텁은 도달 전일 수 있음
        assertThatThrownBy(() -> adminProductService.getMyProductEditForm(productId, otherMemberId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    // 소유권 검증 브랜치를 직접 치고 싶다면: regular를 관리자 역할로 바꿔 validateAdminMember 통과 후 owner mismatch 유도
    @Test
    @DisplayName("소유권 검증 - 관리자인데 다른 사람 상품 수정 시 ACCESS_DENIED")
    void updateProduct_AccessDenied_OwnerMismatch() {
        Long productId = product.getId();
        // regular를 관리자 역할로 변경해서 validateAdminMember 통과시키기
        Member fakeAdmin = Member.builder()
                .Id(99L)
                .accountEmail("fake@admin.com")
                .memberRole(MemberRole.A)
                .memberState(MemberState.A)
                .build();

        when(memberRepository.findById(99L)).thenReturn(Optional.of(fakeAdmin));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product); // product.owner = adminMember

        assertThatThrownBy(() -> adminProductService.updateProduct(
                productId, updateRequest, 99L, null, null, null
        )).isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("validateAdminMember - memberId=null이면 USER_NOT_FOUND")
    void getMyProducts_NullMemberId_Throws() {
        Pageable pageable = PageRequest.of(0, 10);
        assertThatThrownBy(() -> adminProductService.getMyProducts(null, "키워드", "ACTIVE", pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("validateAdminMember - findById 결과 없으면 USER_NOT_FOUND")
    void getMyProducts_MemberNotFound_Throws() {
        Long missingId = 777L;
        Pageable pageable = PageRequest.of(0, 10);
        when(memberRepository.findById(missingId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adminProductService.getMyProducts(missingId, "키워드", "ACTIVE", pageable))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("썸네일 분기 - 새 파일 제공(else-if) + 기존 URL 존재 시 delete 후 upload")
    void updateProduct_Thumbnail_ElseIf_Branch_DeleteAndUpload() {
        Long productId = product.getId();
        Long memberId  = adminMember.getId();

        // 기존 썸네일 세팅(삭제 대상)
        ReflectionTestUtils.setField(product, "thumbnailImageUrl", "https://s3.com/old-thumb.jpg");
        ReflectionTestUtils.setField(product, "thumbnailImageHash", "old-hash");

        MockMultipartFile newThumb = new MockMultipartFile("thumbnail", "new.jpg", "image/jpeg", "NEW".getBytes());

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        // 해시 다르게 해서 실제 교체 경로로 유도
        when(s3Uploader.calculateHash(newThumb)).thenReturn("new-hash");
        when(s3Uploader.upload(eq(newThumb), eq("products/thumbnail"))).thenReturn("https://s3.com/new-thumb.jpg");

        // existingThumbnailHash 아무 값이나 넣어도, 새 파일이 있으므로 else-if 분기 탄다
        AdminProductUpdateRequest req = AdminProductUpdateRequest.builder()
                .productName(updateRequest.productName())
                .description(updateRequest.description())
                .countryName(updateRequest.countryName())
                .hashtags(updateRequest.hashtags())
                .options(updateRequest.options())
                .existingThumbnailHash("anything")
                .existingDetailImageIds(List.of())
                .build();

        adminProductService.updateProduct(productId, req, memberId, newThumb, null, null);

        verify(s3Uploader, times(1)).deleteFile("https://s3.com/old-thumb.jpg");   // if(product.getThumbnailImageUrl()!=null)
        verify(s3Uploader, times(1)).upload(eq(newThumb), eq("products/thumbnail"));
    }

    @Test
    @DisplayName("order 모드 - new 인덱스 음수(new:-1) → IMAGE_UPLOAD_FAILED")
    void updateProduct_OrderMode_NewIndexNegative_Throws() {
        Long productId = product.getId();
        Long memberId  = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        // detailImages 1개지만 new:-1로 음수 인덱스
        assertThatThrownBy(() ->
                adminProductService.updateProduct(
                        productId, updateRequest, memberId,
                        null,
                        List.of(detailImage),
                        List.of("new:-1")
                )
        ).isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("order 모드 - new 인덱스 범위 초과(new:1 when size=1) → IMAGE_UPLOAD_FAILED")
    void updateProduct_OrderMode_NewIndexOutOfRange_Throws() {
        Long productId = product.getId();
        Long memberId  = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        // 리스트 크기 1인데 new:1 요청 → idx >= size 분기
        assertThatThrownBy(() ->
                adminProductService.updateProduct(
                        productId, updateRequest, memberId,
                        null,
                        List.of(detailImage),
                        List.of("new:1")
                )
        ).isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("order 모드 - new 파일 empty → IMAGE_UPLOAD_FAILED (file==null||empty)")
    void updateProduct_OrderMode_NewFileEmpty_Throws() {
        Long productId = product.getId();
        Long memberId  = adminMember.getId();

        MockMultipartFile emptyFile =
                new MockMultipartFile("detail", "empty.jpg", "image/jpeg", new byte[0]);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        // new:0 이지만 해당 파일이 empty → file.isEmpty() true
        assertThatThrownBy(() ->
                adminProductService.updateProduct(
                        productId, updateRequest, memberId,
                        null,
                        List.of(emptyFile),
                        List.of("new:0")
                )
        ).isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("썸네일 - 새 파일 제공(else-if) + 기존 URL 없음 → delete 없이 upload만")
    void updateProduct_Thumbnail_ElseIf_NoPrevUrl() {
        Long productId = product.getId();
        Long memberId  = adminMember.getId();

        // 기존 URL/해시 없도록 세팅 → 내부 if(product.getThumbnailImageUrl()!=null) = false
        ReflectionTestUtils.setField(product, "thumbnailImageUrl", null);
        ReflectionTestUtils.setField(product, "thumbnailImageHash", null);

        MockMultipartFile newThumb = new MockMultipartFile("thumbnail", "new.jpg", "image/jpeg", "NEW".getBytes());

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        when(s3Uploader.calculateHash(newThumb)).thenReturn("new-hash");
        when(s3Uploader.upload(eq(newThumb), eq("products/thumbnail"))).thenReturn("https://s3.com/new-thumb.jpg");

        AdminProductUpdateRequest req = AdminProductUpdateRequest.builder()
                .productName(updateRequest.productName())
                .description(updateRequest.description())
                .countryName(updateRequest.countryName())
                .hashtags(updateRequest.hashtags())
                .options(updateRequest.options())
                .existingThumbnailHash("anything") // else-if 경로만 타게
                .existingDetailImageIds(List.of())
                .build();

        adminProductService.updateProduct(productId, req, memberId, newThumb, null, null);

        // 기존 URL이 null이라 delete 호출 없음
        verify(s3Uploader, never()).deleteFile(anyString());
        verify(s3Uploader, times(1)).upload(eq(newThumb), eq("products/thumbnail"));
    }

    @Test
    @DisplayName("썸네일 - existingThumbnailHash만 있고 새 파일 없음 → 아무 동작 없음(else-if false)")
    void updateProduct_Thumbnail_NoOp_WhenOnlyExistingHash() {
        Long productId = product.getId();
        Long memberId  = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        AdminProductUpdateRequest req = AdminProductUpdateRequest.builder()
                .productName(updateRequest.productName())
                .description(updateRequest.description())
                .countryName(updateRequest.countryName())
                .hashtags(updateRequest.hashtags())
                .options(updateRequest.options())
                .existingThumbnailHash("keep") // thumbnailDeleted = false
                .existingDetailImageIds(List.of())
                .build();

        // 썸네일 파일 null → else-if 조건도 false
        adminProductService.updateProduct(productId, req, memberId, null, List.of(), List.of());

        verify(s3Uploader, never()).deleteFile(anyString());
        verify(s3Uploader, never()).upload(any(MultipartFile.class), anyString());
    }

    @Test
    @DisplayName("썸네일 업데이트 - 삭제 분기(thumbnailDeleted=true) + 기존 URL 존재 → delete 실행")
    void updateProduct_Thumbnail_DeleteBranch_WithPrevUrl() {
        Long productId = product.getId();
        Long memberId  = adminMember.getId();

        // 기존 썸네일 존재 → deleteThumbnailImage(product) 안에서 deleteFile() 호출되도록
        ReflectionTestUtils.setField(product, "thumbnailImageUrl", "https://s3.com/old-thumb.jpg");
        ReflectionTestUtils.setField(product, "thumbnailImageHash", "old-hash");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        // thumbnailDeleted = (thumbnailImage == null && existingThumbnailHash == null)
        AdminProductUpdateRequest req = AdminProductUpdateRequest.builder()
                .productName(updateRequest.productName())
                .description(updateRequest.description())
                .countryName("대한민국")
                .hashtags(updateRequest.hashtags())
                .options(updateRequest.options())
                .existingThumbnailHash(null)      // ← null
                .existingDetailImageIds(List.of())
                .build();

        adminProductService.updateProduct(productId, req, memberId, null, List.of(), List.of());

        // delete 분기 커버
        verify(s3Uploader, times(1)).deleteFile("https://s3.com/old-thumb.jpg");
        // 업로드는 안 함
        verify(s3Uploader, never()).upload(any(MultipartFile.class), anyString());
    }

    @Test
    @DisplayName("썸네일 업데이트 - else-if 분기(새 파일 제공) + 기존 URL 존재 → delete 후 upload")
    void updateProduct_Thumbnail_ElseIf_WithPrevUrl() {
        Long productId = product.getId();
        Long memberId  = adminMember.getId();

        // 기존 URL/해시가 있어야 내부 if(product.getThumbnailImageUrl()!=null)도 true 커버
        ReflectionTestUtils.setField(product, "thumbnailImageUrl", "https://s3.com/old.jpg");
        ReflectionTestUtils.setField(product, "thumbnailImageHash", "old-hash");

        MockMultipartFile newThumb = new MockMultipartFile("thumbnail", "new.jpg", "image/jpeg", "NEW".getBytes());

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        when(s3Uploader.calculateHash(newThumb)).thenReturn("new-hash"); // old-hash 와 다르게
        when(s3Uploader.upload(eq(newThumb), eq("products/thumbnail"))).thenReturn("https://s3.com/new-thumb.jpg");

        AdminProductUpdateRequest req = AdminProductUpdateRequest.builder()
                .productName(updateRequest.productName())
                .description(updateRequest.description())
                .countryName("대한민국")
                .hashtags(updateRequest.hashtags())
                .options(updateRequest.options())
                .existingThumbnailHash("keep")    // ← thumbnailDeleted=false 보장
                .existingDetailImageIds(List.of())
                .build();

        // 새 파일 제공 → else-if 분기 진입
        adminProductService.updateProduct(productId, req, memberId, newThumb, List.of(), List.of());

        // 기존 삭제 + 새 업로드 모두 커버
        verify(s3Uploader, times(1)).deleteFile("https://s3.com/old.jpg");
        verify(s3Uploader, times(1)).upload(eq(newThumb), eq("products/thumbnail"));
    }

    @Test
    @DisplayName("디테일 이미지 order 모드 - new:0가 null 파일 → IMAGE_UPLOAD_FAILED")
    void updateProduct_OrderMode_NewFileNull_Throws() {
        Long productId = product.getId();
        Long memberId  = adminMember.getId();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        // index 0이 null인 리스트 구성
        java.util.ArrayList<MultipartFile> detailImages = new java.util.ArrayList<>();
        detailImages.add(null);

        assertThatThrownBy(() ->
                adminProductService.updateProduct(
                        productId, updateRequest, memberId,
                        null,
                        detailImages,
                        List.of("new:0") // idx=0 → null 파일 참조
                )
        ).isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("썸네일 - 파일은 전달됐지만 empty → else-if 조건(false)로 아무 동작 없음")
    void updateProduct_Thumbnail_ElseIf_Empty_NoOp() {
        Long productId = product.getId();
        Long memberId  = adminMember.getId();

        // empty 파일 준비 (null 아님 + isEmpty() = true)
        MockMultipartFile emptyThumb =
                new MockMultipartFile("thumbnail", "empty.jpg", "image/jpeg", new byte[0]);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(adminMember));
        when(productRepository.findProductWithAllDetailsById(productId)).thenReturn(product);
        when(countryRepository.findByName("대한민국")).thenReturn(Optional.of(country));
        when(productImageRepository.findAllByProduct(product)).thenReturn(List.of());

        // thumbnailDeleted = (thumbnailImage==null && existingThumbnailHash==null) → false가 되도록
        AdminProductUpdateRequest req = AdminProductUpdateRequest.builder()
                .productName(updateRequest.productName())
                .description(updateRequest.description())
                .countryName(updateRequest.countryName())
                .hashtags(updateRequest.hashtags())
                .options(updateRequest.options())
                .existingThumbnailHash("keep")   // 삭제 분기 회피
                .existingDetailImageIds(List.of())
                .build();

        // empty 파일 전달 → else-if 전체가 false로 평가되어 아무 동작도 하지 않아야 함
        adminProductService.updateProduct(productId, req, memberId, emptyThumb, List.of(), List.of());

        // 썸네일 관련 S3 동작이 전혀 없어야 브랜치가 정확히 커버됨
        verify(s3Uploader, never()).deleteFile(anyString());
        verify(s3Uploader, never()).upload(any(MultipartFile.class), anyString());
        verify(s3Uploader, never()).calculateHash(any(MultipartFile.class));
    }
}
