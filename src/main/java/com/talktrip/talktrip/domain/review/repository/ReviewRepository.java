package com.talktrip.talktrip.domain.review.repository;

import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);

    List<Review> findByMemberOrderByCreatedAtDesc(Member member);

    Optional<Review> findByProductIdAndMemberId(Long productId, Long memberId);


}
