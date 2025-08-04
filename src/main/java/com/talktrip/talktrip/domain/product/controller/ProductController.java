package com.talktrip.talktrip.domain.product.controller;

import com.talktrip.talktrip.domain.product.dto.response.ProductDetailResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.service.ProductService;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Tag(name = "Product", description = "상품 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 목록 검색")
    @GetMapping
    public ResponseEntity<List<ProductSummaryResponse>> getProducts(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
            ) {
        return ResponseEntity.ok(productService.searchProducts(keyword, memberDetails, page, size));
    }

    @Operation(summary = "상품 상세 조회")
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomMemberDetails memberDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size) {

        return ResponseEntity.ok(productService.getProductDetail(productId, memberDetails, page, size));
    }

    @Operation(summary = "AI 상품 검색")
    @GetMapping("/aisearch")
    public ResponseEntity<List<ProductSummaryResponse>> aiSearchProducts(
            @RequestParam String question) {
        return ResponseEntity.ok(productService.aiSearchProducts(question));
    }
}