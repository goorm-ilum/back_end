package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.product.dto.response.ProductResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProductsByType(Product.ProductType type) {
        return productRepository.findByType(type).stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> searchProductsByKeyword(String keyword) {
        return productRepository.findByNameContaining(keyword).stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }
}
