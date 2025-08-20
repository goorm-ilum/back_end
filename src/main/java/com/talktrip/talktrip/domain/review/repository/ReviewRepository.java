package com.talktrip.talktrip.domain.review.repository;

import com.talktrip.talktrip.domain.review.entity.Review;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {
    List<Review> findByProductId(Long productId);

    boolean existsByOrderId(Long orderId);

    Page<Review> findByMemberId(Long memberId, Pageable pageable);

    Page<Review> findByProductId(Long productId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId")
    List<Review> findByProductIdIncludingDeleted(@Param("productId") Long productId);



}
