package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    // 1) id로 조회 (삭제 포함)
    @Query(value = "SELECT * FROM product WHERE id = :id", nativeQuery = true)
    Optional<Product> findByIdIncludingDeleted(@Param("id") Long id);

    // 2) id + seller로 조회 (활성만)
    @Query("select p from Product p where p.id = :id and p.member.Id = :sellerId and p.deleted = false")
    Optional<Product> findByIdAndMemberId(@Param("id") Long id, @Param("sellerId") Long sellerId);

    // 3) id + seller로 조회 (삭제 포함)
    @Query("select p from Product p where p.id = :id and p.member.Id = :sellerId")
    Optional<Product> findByIdAndMemberIdIncludingDeleted(@Param("id") Long id, @Param("sellerId") Long sellerId);
}
