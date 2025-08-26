package com.talktrip.talktrip.domain.review.repository;

import com.talktrip.talktrip.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {
    boolean existsByOrderId(Long orderId);

    Page<Review> findByMemberId(Long memberId, Pageable pageable);

    @Query("SELECT r FROM Review r JOIN FETCH r.product WHERE r.member.Id = :memberId")
    Page<Review> findByMemberIdWithProduct(@Param("memberId") Long memberId, Pageable pageable);

    Page<Review> findByProductId(Long productId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId")
    List<Review> findByProductIdIncludingDeleted(@Param("productId") Long productId);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId")
    Page<Review> findByProductIdWithPaging(@Param("productId") Long productId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.reviewStar), 0.0) FROM Review r WHERE r.product.id = :productId")
    Double findAvgStarByProductId(@Param("productId") Long productId);


}
