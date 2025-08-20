package com.talktrip.talktrip.domain.like.repository;

import com.talktrip.talktrip.domain.like.entity.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByProductIdAndMemberId(Long productId, Long memberId);

    Page<Like> findByMemberId(Long memberId, Pageable pageable);

    @Query("select l.product.id from Like l where l.member.id = :memberId and l.product.id in :productIds")
    List<Long> findLikedProductIdsRaw(Long memberId, List<Long> productIds);

    default Set<Long> findLikedProductIds(Long memberId, List<Long> productIds) {
        if (memberId == null || productIds == null || productIds.isEmpty()) return Set.of();
        return new HashSet<>(findLikedProductIdsRaw(memberId, productIds));
    }

    void deleteByProductIdAndMemberId(Long productId, Long memberId);
}