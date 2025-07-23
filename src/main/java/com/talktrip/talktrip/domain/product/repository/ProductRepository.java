package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByType(Product.ProductType type);

    // 이름에 특정 키워드가 포함된 상품 조회 (검색용)
    List<Product> findByNameContaining(String keyword);
}
