package com.talktrip.talktrip.domain.review.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.talktrip.talktrip.domain.review.entity.QReview;
import com.talktrip.talktrip.domain.review.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final Set<String> ALLOWED_SORT = Set.of(
            "reviewStar", "updatedAt"
    );

    @Override
    public Page<Review> findByProductIdWithPaging(Long productId, Pageable pageable) {
        QReview r = QReview.review;

        JPAQuery<Review> query = queryFactory
                .selectFrom(r)
                .where(r.product.id.eq(productId));

        if (pageable.getSort().isUnsorted()) {
            query.orderBy(r.updatedAt.desc());
        } else {
            for (Sort.Order o : pageable.getSort()) {
                boolean asc = o.getDirection() == Sort.Direction.ASC;
                String prop = o.getProperty();
                switch (prop) {
                    case "reviewStar" -> query.orderBy(asc ? r.reviewStar.asc() : r.reviewStar.desc());
                    case "updatedAt"  -> query.orderBy(asc ? r.updatedAt.asc()  : r.updatedAt.desc());
                    default -> throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Unsupported sort property: " + prop
                    );
                }
            }
        }

        List<Review> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = java.util.Objects.requireNonNullElse(
                queryFactory
                        .select(r.id.count())
                        .from(r)
                        .where(r.product.id.eq(productId))
                        .fetchOne(),
                0L
        );

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<Review> findByMemberIdWithProduct(Long memberId, Pageable pageable) {
        QReview r = QReview.review;

        JPAQuery<Review> query = queryFactory
                .selectFrom(r)
                .leftJoin(r.product).fetchJoin()
                .where(r.member.Id.eq(memberId));

        if (pageable.getSort().isUnsorted()) {
            query.orderBy(r.updatedAt.desc());
        } else {
            for (Sort.Order o : pageable.getSort()) {
                boolean asc = o.getDirection() == Sort.Direction.ASC;
                String prop = o.getProperty();
                switch (prop) {
                    case "reviewStar" -> query.orderBy(asc ? r.reviewStar.asc() : r.reviewStar.desc());
                    case "updatedAt"  -> query.orderBy(asc ? r.updatedAt.asc()  : r.updatedAt.desc());
                    default -> throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Unsupported sort property: " + prop
                    );
                }
            }
        }

        List<Review> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = java.util.Objects.requireNonNullElse(
                queryFactory
                        .select(r.id.count())
                        .from(r)
                        .where(r.member.Id.eq(memberId))
                        .fetchOne(),
                0L
        );

        return new PageImpl<>(content, pageable, total);
    }

}