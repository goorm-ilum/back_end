package com.talktrip.talktrip.domain.product.controller;

import com.talktrip.talktrip.domain.product.dto.request.AdminProductCreateRequest;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductUpdateRequest;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductEditResponse;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductSummaryResponse;
import com.talktrip.talktrip.domain.product.service.AdminProductService;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.talktrip.talktrip.global.util.SortUtil.buildSort;

@Tag(name = "Admin Product", description = "판매자 상품 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final AdminProductService adminProductService;

    @Operation(summary = "판매자 상품 등록")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createProduct(
            @Valid @RequestPart("request") AdminProductCreateRequest request,
            @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnailImage,
            @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        adminProductService.createProduct(request, memberDetails.getId(), thumbnailImage, detailImages);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "판매자 상품 목록 조회 + 검색 + 정렬")
    @GetMapping
    public ResponseEntity<Page<AdminProductSummaryResponse>> getMyProducts(
            @AuthenticationPrincipal CustomMemberDetails memberDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt,desc") List<String> sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        return ResponseEntity.ok(adminProductService.getMyProducts(memberDetails.getId(), keyword, status, pageable));
    }

    @Operation(summary = "판매자 상품 상세 조회")
    @GetMapping("/{productId}")
    public ResponseEntity<AdminProductEditResponse> getProductDetail(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        return ResponseEntity.ok(adminProductService.getMyProductEditForm(productId, memberDetails.getId()));
    }

    @Operation(summary = "판매자 상품 수정 (기존+신규 이미지를 최종 순서로 반영)")
    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestPart("request") AdminProductUpdateRequest request,
            @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnailImage,
            @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages,
            @RequestPart(value = "detailImageOrder", required = false) List<String> detailImageOrder,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        adminProductService.updateProduct(productId, request, memberDetails.getId(),
                thumbnailImage, detailImages, detailImageOrder);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "판매자 상품 삭제")
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        adminProductService.deleteProduct(productId, memberDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "소프트 삭제된 상품 복구")
    @PostMapping("/{productId}/restore")
    public ResponseEntity<Void> restore(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomMemberDetails seller
    ) {
        adminProductService.restoreProduct(productId, seller.getId());
        return ResponseEntity.noContent().build();
    }
}
