package com.talktrip.talktrip.domain.review.repository;

import com.talktrip.talktrip.domain.review.entity.ReviewKeyword;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewKeywordRepository extends MongoRepository<ReviewKeyword, String>{

    List<ReviewKeyword> findByReviewId(int reviewId);
    List<ReviewKeyword> findByProductId(int productId);

    // polarity 별로 count
    long countByProductIdAndPolarity(int productId, int polarity);

}
