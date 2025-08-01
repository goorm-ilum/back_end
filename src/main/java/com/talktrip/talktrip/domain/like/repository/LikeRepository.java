package com.talktrip.talktrip.domain.like.repository;

import com.talktrip.talktrip.domain.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByProductIdAndMemberId(Long productId, Long memberId);

    List<Like> findByMemberId(Long MemberId);

    void deleteByProductIdAndMemberId(Long productId, Long memberId);
}