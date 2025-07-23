package com.talktrip.talktrip.domain.product.controller;

import com.talktrip.talktrip.domain.product.dto.response.ProductResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.service.ProductService;
import com.talktrip.talktrip.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/type")
    public List<ProductResponse> getProductsByType(@RequestParam Product.ProductType type) {
        return productService.getProductsByType(type);
    }

    @GetMapping("/search")
    public List<ProductResponse> searchProducts(@RequestParam String keyword) {
        return productService.searchProductsByKeyword(keyword);
    }
}