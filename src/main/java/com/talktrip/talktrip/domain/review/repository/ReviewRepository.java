package com.talktrip.talktrip.domain.review.repository;

import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);

    Page<Review> findByMember(Member member, Pageable pageable);

    Page<Review> findByProductId(Long productId, Pageable pageable);

    Optional<Review> findByProductIdAndMemberId(Long productId, Long memberId);


}
