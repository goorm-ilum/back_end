package com.talktrip.talktrip.domain.like.repository;

import com.talktrip.talktrip.domain.like.dto.ProductWithAvgStar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LikeRepositoryCustom {
    Page<ProductWithAvgStar> findLikedProductsWithAvgStar(Long memberId, Pageable pageable);
}
