package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    @Query(value = "SELECT * FROM product WHERE id = :id", nativeQuery = true)
    Optional<Product> findByIdIncludingDeleted(@Param("id") Long id);

    @Query("select p from Product p where p.id = :id and p.member.Id = :sellerId")
    Optional<Product> findByIdAndMemberIdIncludingDeleted(@Param("id") Long id, @Param("sellerId") Long sellerId);

    @Query("""
            SELECT new com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse(
                p.id, p.productName, p.description, p.thumbnailImageUrl,
                MIN(po.price), MIN(po.discountPrice),
                COALESCE(AVG(r.reviewStar), 0.0), true, null
            )
            FROM Product p
            LEFT JOIN ProductOption po ON p.id = po.product.id
            LEFT JOIN Review r ON p.id = r.product.id
            WHERE p.id IN :productIds
            GROUP BY p.id, p.productName, p.description, p.thumbnailImageUrl
            """)
    List<ProductSummaryResponse> findProductSummariesByIds(@Param("productIds") List<Long> productIds);

    @Query("""
            SELECT new com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse(
                p.id, p.productName, p.description, p.thumbnailImageUrl,
                MIN(po.price), MIN(po.discountPrice),
                COALESCE(AVG(r.reviewStar), 0.0), false
            )
            FROM Product p
            LEFT JOIN p.country c
            LEFT JOIN ProductOption po ON p.id = po.product.id 
                AND po.startDate >= :tomorrow AND po.stock > 0
            LEFT JOIN Review r ON p.id = r.product.id
            WHERE p.deleted = false
            AND (:countryName IS NULL OR :countryName = '전체' OR c.name = :countryName)
            AND (:keyword IS NULL OR :keyword = '' OR p.productName LIKE %:keyword%)
            GROUP BY p.id, p.productName, p.description, p.thumbnailImageUrl
            HAVING MIN(po.price) IS NOT NULL
            ORDER BY p.updatedAt DESC
            """)
    Page<ProductSummaryResponse> searchProductsWithStock(
            @Param("keyword") String keyword,
            @Param("countryName") String countryName,
            @Param("tomorrow") LocalDate tomorrow,
            Pageable pageable
    );

    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.productOptions po
            LEFT JOIN FETCH p.images
            LEFT JOIN FETCH p.reviews r
            LEFT JOIN FETCH r.member
            LEFT JOIN FETCH p.hashtags
            LEFT JOIN FETCH p.country
            WHERE p.id = :productId
            AND p.deleted = false
            AND EXISTS (
                SELECT 1 FROM ProductOption po2
                WHERE po2.product.id = p.id
                AND po2.startDate >= :tomorrow
                AND po2.stock > 0
            )
            """)
    Optional<Product> findByIdWithDetailsAndStock(@Param("productId") Long productId, @Param("tomorrow") LocalDate tomorrow);
}
