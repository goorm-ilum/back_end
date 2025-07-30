package com.talktrip.talktrip.domain.product.controller;

import com.talktrip.talktrip.domain.product.dto.request.AdminProductCreateRequest;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductEditResponse;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductSummaryResponse;
import com.talktrip.talktrip.domain.product.service.AdminProductService;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final AdminProductService adminProductService;

    @Operation(summary = "판매자 상품 등록")
    @PostMapping
    public ResponseEntity<Void> createProduct(
            @RequestBody AdminProductCreateRequest request,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        adminProductService.createProduct(request, memberDetails.getId());
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
    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateProduct(
            @PathVariable Long productId,
            @RequestBody AdminProductCreateRequest request,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        adminProductService.updateProduct(productId, request, memberDetails.getId());
        return ResponseEntity.ok().build();
    }
}
