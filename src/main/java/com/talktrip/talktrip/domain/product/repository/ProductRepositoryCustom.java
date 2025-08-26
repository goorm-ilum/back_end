package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.product.dto.ProductWithAvgStarAndLike;
import com.talktrip.talktrip.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductRepositoryCustom {
    Page<Product> findSellerProducts(
            Long sellerId,
            String status,
            String keyword,
            Pageable pageable);

    Page<ProductWithAvgStarAndLike> searchProductsWithAvgStarAndLike(
            String keyword,
            String countryName,
            LocalDate tomorrow,
            Long memberId,
            Pageable pageable
    );

    Optional<ProductWithAvgStarAndLike> findByIdWithDetailsAndAvgStarAndLike(
            Long productId,
            LocalDate tomorrow,
            Long memberId
    );

    Page<ProductWithAvgStarAndLike> findProductsWithAvgStarAndLikeByIds(
            List<Long> productIds,
            Long memberId,
            Pageable pageable
    );
    
    Product findProductWithAllDetailsById(Long productId);
}
