package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductRepositoryCustom {
    Page<Product> searchByKeywords(List<String> keywords, String countryName, Pageable pageable);

    Page<Product> findVisibleProducts(String countryName, Pageable pageable);

    Page<Product> findSellerProducts(Long sellerId, String status, String keyword, Pageable pageable);
}
