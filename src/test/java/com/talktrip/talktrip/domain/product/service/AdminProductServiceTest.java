package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductCreateRequest;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductUpdateRequest;
import com.talktrip.talktrip.domain.product.dto.request.ProductOptionRequest;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductEditResponse;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import static com.talktrip.talktrip.global.TestConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @InjectMocks AdminProductService adminProductService;

    @Mock ProductRepository productRepository;
    @Mock CountryRepository countryRepository;
    @Mock MemberRepository memberRepository;
    @Mock ProductImageRepository productImageRepository;
    @Mock ProductHashTagRepository productHashTagRepository;
    @Mock ProductOptionRepository productOptionRepository;
    @Mock S3Uploader s3Uploader;

    private Member seller() {
        return Member.builder()
                .Id(SELLER_ID)
                .name(SELLER_NAME)
                .accountEmail(SELLER_EMAIL)
                .phoneNum(PHONE_NUMBER)
                .build();
    }

    private Country kr() {
        return Country.builder().name(COUNTRY_KOREA).continent(CONTINENT_ASIA).build();
    }

    private Product ownedProduct() {
        Product p = Product.builder()
                .id(PRODUCT_ID)
                .member(seller())
                .productName(PRODUCT_NAME_1)
                .description(DESC)
                .country(kr())
                .thumbnailImageUrl(THUMBNAIL_URL)
                .thumbnailImageHash(THUMBNAIL_HASH)
                .deleted(false)
                .build();

        p.getImages().add(ProductImage.builder()
                .id(IMAGE_ID_1)
                .product(p)
                .imageUrl(IMAGE_URL_1)
                .sortOrder(0)
                .build());

        p.getHashtags().add(HashTag.builder().product(p).hashtag(HASHTAG_SEA).build());

        p.getProductOptions().add(ProductOption.builder()
                .id(OPTION_ID_1)
                .product(p)
                .optionName(OPTION_NAME)
                .startDate(LocalDate.now())
                .stock(STOCK_3)
                .price(PRICE_10000)
                .discountPrice(DISC_9000)
                .build());

        return p;
    }

    @Nested @DisplayName("createProduct(request, memberId, thumbnail, details)")
    class Create {

        @Test @DisplayName("정상 생성(썸네일/디테일 이미지 포함)")
        void ok_with_images() {
            AdminProductCreateRequest req = new AdminProductCreateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA,
                    List.of(new ProductOptionRequest(LocalDate.now(), OPTION_NAME, STOCK_5, PRICE_10000, DISC_9000)),
                    List.of(HASHTAG_SEA, HASHTAG_FOOD)
            );
            MockMultipartFile thumb = new MockMultipartFile(MF_T_NAME, MF_T_FILENAME, MEDIA_IMAGE_PNG, BYTES_X);
            MockMultipartFile d1 = new MockMultipartFile(MF_D_NAME, MF_D1_FILENAME, MEDIA_IMAGE_PNG, BYTES_X);

            given(memberRepository.findById(SELLER_ID)).willReturn(Optional.of(seller()));
            given(countryRepository.findByName(COUNTRY_KOREA)).willReturn(Optional.of(kr()));
            given(s3Uploader.upload(any(MultipartFile.class), anyString())).willReturn(THUMBNAIL_URL);
            given(s3Uploader.calculateHash(any(MultipartFile.class))).willReturn(THUMBNAIL_HASH);

            adminProductService.createProduct(req, SELLER_ID, thumb, List.of(d1));

            then(productRepository).should().save(argThat(p ->
                    p.getProductOptions().size() == 1 &&
                            p.getImages().size() == 1 &&
                            THUMBNAIL_URL.equals(p.getThumbnailImageUrl()) &&
                            THUMBNAIL_HASH.equals(p.getThumbnailImageHash())
            ));
        }

        @Test @DisplayName("ADMIN_NOT_FOUND")
        void adminNotFound() {
            given(memberRepository.findById(SELLER_ID)).willReturn(Optional.empty());
            AdminProductCreateRequest req = new AdminProductCreateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA, List.of(), List.of());

            assertThatThrownBy(() ->
                    adminProductService.createProduct(req, SELLER_ID, null, null)
            ).isInstanceOf(MemberException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.ADMIN_NOT_FOUND);
        }

        @Test @DisplayName("COUNTRY_NOT_FOUND")
        void countryNotFound() {
            given(memberRepository.findById(SELLER_ID)).willReturn(Optional.of(seller()));
            given(countryRepository.findByName(COUNTRY_KOREA)).willReturn(Optional.empty());

            AdminProductCreateRequest req = new AdminProductCreateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA, List.of(), List.of());

            assertThatThrownBy(() ->
                    adminProductService.createProduct(req, SELLER_ID, null, null)
            ).isInstanceOf(ProductException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.COUNTRY_NOT_FOUND);
        }
    }

    @Nested @DisplayName("getMyProducts(memberId, keyword, status, pageable)")
    class MyList {

        @Test @DisplayName("status null → 기본 ACTIVE로 위임, 페이지/매핑 정상")
        void defaultActive_noKeyword() {
            Product p = ownedProduct();
            Pageable pageable = PAGE_0_SIZE_10;

            given(productRepository.findSellerProducts(eq(SELLER_ID), eq(STATUS_ACTIVE), isNull(), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(p), pageable, 1));

            Page<AdminProductSummaryResponse> res =
                    adminProductService.getMyProducts(SELLER_ID, null, null, pageable);

            assertThat(res.getTotalElements()).isEqualTo(1);
            assertThat(res.getContent().getFirst().id()).isEqualTo(PRODUCT_ID);

            then(productRepository).should().findSellerProducts(SELLER_ID, STATUS_ACTIVE, null, pageable);
        }

        @Test @DisplayName("키워드/정렬 전달 위임 확인")
        void keyword_and_sort_passthrough() {
            Product a = ownedProduct();
            Product b = ownedProduct();
            b.updateBasicInfo(PRODUCT_NAME_3, DESC, kr());

            Pageable pageable = PageRequest.of(PAGE_0, SIZE_10, SORT_BY_PRODUCT_NAME_DESC);

            given(productRepository.findSellerProducts(eq(SELLER_ID), eq(STATUS_ACTIVE), eq(KEYWORD_P), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(a, b), pageable, 2));

            Page<AdminProductSummaryResponse> res =
                    adminProductService.getMyProducts(SELLER_ID, KEYWORD_P, STATUS_ACTIVE, pageable);

            assertThat(res.getContent()).extracting(AdminProductSummaryResponse::productName)
                    .contains(PRODUCT_NAME_1, PRODUCT_NAME_3);

            then(productRepository).should().findSellerProducts(SELLER_ID, STATUS_ACTIVE, KEYWORD_P, pageable);
        }
    }

    @Nested @DisplayName("getMyProductEditForm(productId, memberId)")
    class EditForm {

        @Test @DisplayName("정상 반환")
        void ok() {
            Product p = ownedProduct();
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.of(p));

            AdminProductEditResponse res = adminProductService.getMyProductEditForm(PRODUCT_ID, SELLER_ID);

            assertThat(res.productName()).isEqualTo(PRODUCT_NAME_1);
            assertThat(res.options()).hasSize(1);
        }

        @Test @DisplayName("PRODUCT_NOT_FOUND")
        void notFound() {
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    adminProductService.getMyProductEditForm(PRODUCT_ID, SELLER_ID)
            ).isInstanceOf(ProductException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test @DisplayName("ACCESS_DENIED (소유자 불일치)")
        void accessDenied() {
            Product p = ownedProduct();
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID))
                    .willReturn(Optional.of(p));

            assertThatThrownBy(() ->
                    adminProductService.getMyProductEditForm(PRODUCT_ID, OTHER_MEMBER_ID)
            ).isInstanceOf(ProductException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }

    @Nested @DisplayName("updateProduct(productId, request, memberId, thumbnail, details, order)")
    class Update {

        @Test @DisplayName("정상(권한 OK) - fallback 경로(exitingDetailImageIds 사용), 삭제 없음")
        void ok() {
            Product p = ownedProduct();
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.of(p));
            given(countryRepository.findByName(COUNTRY_KOREA)).willReturn(Optional.of(kr()));
            given(productImageRepository.findAllByProduct(p)).willReturn(p.getImages());

            AdminProductUpdateRequest req = new AdminProductUpdateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA,
                    List.of(new ProductOptionRequest(LocalDate.now(), OPTION_NAME, STOCK_3, PRICE_10000, DISC_9000)),
                    List.of(HASHTAG_SEA), THUMBNAIL_HASH, List.of(IMAGE_ID_1)
            );

            adminProductService.updateProduct(PRODUCT_ID, req, SELLER_ID, null, List.of(), null);

            then(productImageRepository).should(never()).delete(any());
            then(productHashTagRepository).should().deleteAllByProduct(p);
            then(productOptionRepository).should().deleteAllByProduct(p);
        }

        @Test @DisplayName("PRODUCT_NOT_FOUND")
        void notFound() {
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.empty());

            AdminProductUpdateRequest req = new AdminProductUpdateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA, List.of(), List.of(), THUMBNAIL_HASH, List.of());

            assertThatThrownBy(() ->
                    adminProductService.updateProduct(PRODUCT_ID, req, SELLER_ID, null, null, null)
            ).isInstanceOf(ProductException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test @DisplayName("ACCESS_DENIED (소유자 불일치)")
        void accessDenied() {
            Product p = ownedProduct();
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.of(p));

            AdminProductUpdateRequest req = new AdminProductUpdateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA, List.of(), List.of(), THUMBNAIL_HASH, List.of());

            assertThatThrownBy(() ->
                    adminProductService.updateProduct(PRODUCT_ID, req, OTHER_MEMBER_ID, null, null, null)
            ).isInstanceOf(ProductException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test @DisplayName("COUNTRY_NOT_FOUND")
        void countryNotFound() {
            Product p = ownedProduct();
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.of(p));
            given(countryRepository.findByName(COUNTRY_KOREA)).willReturn(Optional.empty());

            AdminProductUpdateRequest req = new AdminProductUpdateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA, List.of(), List.of(), THUMBNAIL_HASH, List.of());

            assertThatThrownBy(() ->
                    adminProductService.updateProduct(PRODUCT_ID, req, SELLER_ID, null, null, null)
            ).isInstanceOf(ProductException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.COUNTRY_NOT_FOUND);
        }

        @Test @DisplayName("IMAGE_UPLOAD_FAILED - new:N 토큰이 파일 범위 밖")
        void imageUploadFailed_badToken() {
            Product p = ownedProduct();
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.of(p));
            given(countryRepository.findByName(COUNTRY_KOREA)).willReturn(Optional.of(kr()));
            given(productImageRepository.findAllByProduct(p)).willReturn(p.getImages());

            AdminProductUpdateRequest req = new AdminProductUpdateRequest(
                    PRODUCT_NAME_1, DESC, COUNTRY_KOREA, List.of(), List.of(), THUMBNAIL_HASH, List.of());

            assertThatThrownBy(() ->
                    adminProductService.updateProduct(PRODUCT_ID, req, SELLER_ID, null, List.of(), List.of("new:1"))
            ).isInstanceOf(ProductException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    @Nested @DisplayName("deleteProduct(productId, memberId)")
    class Delete {

        @Test @DisplayName("정상 소프트 삭제")
        void ok() {
            Product p = ownedProduct();
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.of(p));

            adminProductService.deleteProduct(PRODUCT_ID, SELLER_ID);

            assertThat(p.isDeleted()).isTrue();
        }

        @Test @DisplayName("PRODUCT_NOT_FOUND")
        void notFound() {
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    adminProductService.deleteProduct(PRODUCT_ID, SELLER_ID)
            ).isInstanceOf(ProductException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test @DisplayName("ACCESS_DENIED (소유자 불일치)")
        void accessDenied() {
            Product p = ownedProduct(); // 소유자는 SELLER_ID
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID)).willReturn(Optional.of(p));

            assertThatThrownBy(() ->
                    adminProductService.deleteProduct(PRODUCT_ID, OTHER_MEMBER_ID)
            ).isInstanceOf(ProductException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }

    @Nested @DisplayName("restoreProduct(productId, memberId)")
    class Restore {

        @Test @DisplayName("정상 복구")
        void ok() {
            Product p = ownedProduct();
            p.markDeleted();
            assertThat(p.isDeleted()).isTrue();

            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID))
                    .willReturn(Optional.of(p));

            adminProductService.restoreProduct(PRODUCT_ID, SELLER_ID);

            assertThat(p.isDeleted()).isFalse();
        }

        @Test @DisplayName("PRODUCT_NOT_FOUND")
        void notFound() {
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    adminProductService.restoreProduct(PRODUCT_ID, SELLER_ID)
            ).isInstanceOf(ProductException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test @DisplayName("ACCESS_DENIED (소유자 불일치)")
        void accessDenied() {
            Product p = ownedProduct(); // 소유자는 SELLER_ID
            p.markDeleted();
            given(productRepository.findByIdIncludingDeleted(PRODUCT_ID))
                    .willReturn(Optional.of(p));

            assertThatThrownBy(() ->
                    adminProductService.restoreProduct(PRODUCT_ID, OTHER_MEMBER_ID)
            ).isInstanceOf(ProductException.class)
                    .extracting(ERROR_CODE).isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }
}
