package com.talktrip.talktrip.domain.like.repository;

import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByProductIdAndMemberId(Long productId, Long memberId);

    Page<Like> findByMemberId(Long memberId, Pageable pageable);

    @Query("""
            SELECT new com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse(
                p.id, p.productName, p.description, p.thumbnailImageUrl,
                MIN(po.price), MIN(po.discountPrice),
                COALESCE(AVG(r.reviewStar), 0.0), true, l.createdAt
            )
            FROM Like l
            JOIN Product p ON l.productId = p.id
            LEFT JOIN ProductOption po ON p.id = po.product.id
            LEFT JOIN Review r ON p.id = r.product.id
            WHERE l.memberId = :memberId 
            AND p.deleted = false
            GROUP BY p.id, p.productName, p.description, p.thumbnailImageUrl, l.createdAt
            """)
    Page<ProductSummaryResponse> findLikedProductSummaries(Long memberId, Pageable pageable);

    @Query("select l.productId from Like l where l.memberId = :memberId and l.productId in :productIds")
    Set<Long> findLikedProductIdsRaw(Long memberId, List<Long> productIds);

    default Set<Long> findLikedProductIds(Long memberId, List<Long> productIds) {
        if (memberId == null || productIds == null || productIds.isEmpty()) return Set.of();
        return findLikedProductIdsRaw(memberId, productIds);
    }

    void deleteByProductIdAndMemberId(Long productId, Long memberId);
}