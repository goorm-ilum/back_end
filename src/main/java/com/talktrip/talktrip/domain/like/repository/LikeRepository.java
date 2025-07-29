package com.talktrip.talktrip.domain.like.repository;

import com.talktrip.talktrip.domain.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByProductIdAndBuyerId(Long productId, Long userId);

    List<Like> findByBuyerId(Long buyerId);

    void deleteByProductIdAndBuyerId(Long productId, Long buyerId);
}