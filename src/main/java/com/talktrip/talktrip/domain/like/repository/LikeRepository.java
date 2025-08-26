package com.talktrip.talktrip.domain.like.repository;

import com.talktrip.talktrip.domain.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface LikeRepository extends JpaRepository<Like, Long>, LikeRepositoryCustom {
    boolean existsByProductIdAndMemberId(Long productId, Long memberId);

    @Query("select l.productId from Like l where l.memberId = :memberId and l.productId in :productIds")
    Set<Long> findLikedProductIdsRaw(@Param("memberId") Long memberId, @Param("productIds") List<Long> productIds);

    default Set<Long> findLikedProductIds(Long memberId, List<Long> productIds) {
        if (memberId == null || productIds == null || productIds.isEmpty()) return Set.of();
        return findLikedProductIdsRaw(memberId, productIds);
    }

    void deleteByProductIdAndMemberId(Long productId, Long memberId);
}