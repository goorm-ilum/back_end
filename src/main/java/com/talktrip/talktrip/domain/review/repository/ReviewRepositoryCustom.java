package com.talktrip.talktrip.domain.review.repository;

import java.util.List;
import java.util.Map;

public interface ReviewRepositoryCustom {
    Map<Long, Double> fetchAvgStarsByProductIds(List<Long> productIds);
}
