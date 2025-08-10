package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.product.entity.Product;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {
    List<Product> findByCountryName(String countryName);

    @Query(value = "SELECT * FROM product WHERE id = :id", nativeQuery = true)
    Optional<Product> findByIdIncludingDeleted(@Param("id") Long id);

    @Query(value = "SELECT * FROM product WHERE id = :id AND seller_id = :sellerId", nativeQuery = true)
    Optional<Product> findByIdAndMemberId(@Param("id") Long id, @Param("sellerId") Long sellerId);

    @Query(value = "SELECT * FROM product WHERE id = :id AND seller_id = :sellerId", nativeQuery = true)
    Optional<Product> findByIdAndMemberIdIncludingDeleted(@Param("id") Long id, @Param("sellerId") Long sellerId);

    @Query(value = """
       SELECT * FROM product
       WHERE seller_id = :sellerId
         AND (:status = 'ALL'
              OR (:status = 'ACTIVE' AND deleted = false)
              OR (:status = 'DELETED' AND deleted = true))
       ORDER BY updated_at DESC
    """, nativeQuery = true)
    List<Product> findBySellerWithStatus(@Param("sellerId") Long sellerId,
                                         @Param("status") String status);
}
