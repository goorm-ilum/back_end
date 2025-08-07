package com.talktrip.talktrip.domain.product.controller;

import com.talktrip.talktrip.domain.product.dto.response.ProductDetailResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.service.ProductService;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.talktrip.talktrip.global.util.SortUtil.buildSort;

@Slf4j
@Tag(name = "Product", description = "상품 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 목록 검색")
    @GetMapping
    public ResponseEntity<Page<ProductSummaryResponse>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt,desc") List<String> sort,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        return ResponseEntity.ok(productService.searchProducts(keyword, memberDetails, pageable));
    }

    @Operation(summary = "상품 상세 조회")
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomMemberDetails memberDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "createdAt,desc") List<String> sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        return ResponseEntity.ok(productService.getProductDetail(productId, memberDetails, pageable));
    }

    @Operation(summary = "AI 상품 검색")
    @GetMapping("/aisearch")
    public ResponseEntity<List<ProductSummaryResponse>> aiSearchProducts(
            @RequestParam String question) {
        return ResponseEntity.ok(productService.aiSearchProducts(question));
    }
}