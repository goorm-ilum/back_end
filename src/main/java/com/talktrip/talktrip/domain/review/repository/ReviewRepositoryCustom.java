package com.talktrip.talktrip.domain.review.repository;

import com.talktrip.talktrip.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewRepositoryCustom {
    Page<Review> findByProductIdWithPaging(Long productId, Pageable pageable);
    
    Page<Review> findByMemberIdWithProduct(Long memberId, Pageable pageable);
}
