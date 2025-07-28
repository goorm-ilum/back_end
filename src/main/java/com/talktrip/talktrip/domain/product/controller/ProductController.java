package com.talktrip.talktrip.domain.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product", description = "상품 관련 API")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Operation(summary = "상품 목록 조회")
    @GetMapping
    public void getProducts() {}

    @Operation(summary = "상품 상세 조회")
    @GetMapping("/{productId}")
    public void getProductDetail(@PathVariable Long productId) {}
}