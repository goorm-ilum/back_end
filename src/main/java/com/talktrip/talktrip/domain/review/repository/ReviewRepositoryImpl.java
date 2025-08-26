package com.talktrip.talktrip.domain.review.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.talktrip.talktrip.domain.review.entity.QReview;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Long, Double> fetchAvgStarsByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Map.of();

        QReview r = QReview.review;

        List<Tuple> tuples = queryFactory
                .select(r.product.id, r.reviewStar.avg())
                .from(r)
                .where(r.product.id.in(productIds))
                .groupBy(r.product.id)
                .fetch();

        Map<Long, Double> result = new HashMap<>();
        for (Tuple t : tuples) {
            Long productId = t.get(r.product.id);
            Double avg = t.get(r.reviewStar.avg());
            double rounded = (avg == null) ? 0.0 : Math.round(avg * 10.0) / 10.0;
            result.put(productId, rounded);
        }
        return result;
    }
}