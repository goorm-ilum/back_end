package com.talktrip.talktrip.domain.like.repository;

import com.talktrip.talktrip.domain.like.entity.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByProductIdAndMemberId(Long productId, Long memberId);

    Page<Like> findByMemberId(Long memberId, Pageable pageable);

    void deleteByProductIdAndMemberId(Long productId, Long memberId);
}