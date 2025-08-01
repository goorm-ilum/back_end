package com.talktrip.talktrip.domain.product.controller;

import com.talktrip.talktrip.domain.product.dto.request.AdminProductCreateRequest;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductEditResponse;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductSummaryResponse;
import com.talktrip.talktrip.domain.product.service.AdminProductService;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final AdminProductService adminProductService;

    @Operation(summary = "판매자 상품 등록")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createProduct(
            @RequestPart("request") AdminProductCreateRequest request,
            @RequestPart("thumbnailImage") MultipartFile thumbnailImage,
            @RequestPart("detailImages") List<MultipartFile> detailImages,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        adminProductService.createProduct(request, 1L, thumbnailImage, detailImages);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "판매자 상품 목록 조회")
    @GetMapping
    public ResponseEntity<List<AdminProductSummaryResponse>> getMyProducts(
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        return ResponseEntity.ok(adminProductService.getMyProducts(memberDetails.getId()));
    }

    @Operation(summary = "판매자 상품 상세 조회")
    @GetMapping("/{productId}")
    public ResponseEntity<AdminProductEditResponse> getProductDetail(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        return ResponseEntity.ok(adminProductService.getMyProductEditForm(productId, memberDetails.getId()));
    }

    @Operation(summary = "판매자 상품 수정")
    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateProduct(
            @PathVariable Long productId,
            @RequestPart("request") AdminProductCreateRequest request,
            @RequestPart("thumbnailImage") MultipartFile thumbnailImage,
            @RequestPart("detailImages") List<MultipartFile> detailImages,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        adminProductService.updateProduct(productId, request, 1L, thumbnailImage, detailImages);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "판매자 상품 삭제")
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        adminProductService.deleteProduct(productId, 1L);
        return ResponseEntity.noContent().build();
    }
}
