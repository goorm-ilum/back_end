package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.product.entity.Product;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {
    List<Product> findByMemberId(Long memberId);

    Optional<Product> findByIdAndMemberId(Long id, Long memberId);

    @Query("SELECT p FROM Product p WHERE p.member.Id = :memberId ORDER BY p.id DESC")
    List<Product> findByMemberId(@Param("memberId") Long memberId, @Param("offset") int offset, @Param("size") int size);
}
