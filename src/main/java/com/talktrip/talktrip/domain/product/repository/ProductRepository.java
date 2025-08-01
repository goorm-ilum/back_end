package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {
    List<Product> findByMemberId(Long memberId);

    Optional<Product> findByIdAndMemberId(Long id, Long memberId);
}
