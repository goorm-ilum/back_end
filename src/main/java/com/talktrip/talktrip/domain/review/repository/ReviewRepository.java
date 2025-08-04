package com.talktrip.talktrip.domain.review.repository;

import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);

    List<Review> findByMemberOrderByCreatedAtDesc(Member member);

}
