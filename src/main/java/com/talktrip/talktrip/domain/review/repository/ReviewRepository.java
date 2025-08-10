package com.talktrip.talktrip.domain.review.repository;

import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.member.entity.Member;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);

    boolean existsByOrderId(Long orderId);

    Page<Review> findByMemberId(Long memberId, Pageable pageable);

    Page<Review> findByProductId(Long productId, Pageable pageable);


}
