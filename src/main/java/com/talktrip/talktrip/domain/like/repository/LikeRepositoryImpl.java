package com.talktrip.talktrip.domain.like.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.talktrip.talktrip.domain.like.dto.ProductWithAvgStar;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.talktrip.talktrip.domain.like.entity.QLike.like;
import static com.talktrip.talktrip.domain.product.entity.QProduct.product;
import static com.talktrip.talktrip.domain.review.entity.QReview.review;

@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final Set<String> ALLOWED_SORT = Set.of(
            "updatedAt", "averageReviewStar"
    );

    @Override
    public Page<ProductWithAvgStar> findLikedProductsWithAvgStar(Long memberId, Pageable pageable) {
        JPAQuery<ProductWithAvgStar> query = queryFactory
                .select(Projections.constructor(ProductWithAvgStar.class,
                        product,
                        review.reviewStar.avg().coalesce(Expressions.constant(0.0))
                ))
                .from(like)
                .join(product).on(like.productId.eq(product.id))
                .leftJoin(review).on(product.id.eq(review.product.id))
                .where(like.memberId.eq(memberId)
                        .and(product.deleted.eq(false)))
                .groupBy(product.id, product.productName, product.description, 
                        product.thumbnailImageUrl, product.thumbnailImageHash, product.member, 
                        product.country, product.deleted, product.deletedAt, product.createdAt, product.updatedAt);

        // 정렬 적용
        if (pageable.getSort().isUnsorted()) {
            query.orderBy(product.updatedAt.desc());
        } else {
            for (Sort.Order order : pageable.getSort()) {
                String property = order.getProperty();
                if (!ALLOWED_SORT.contains(property)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Unsupported sort property: " + property
                    );
                }

                // ALLOWED_SORT 에 의해 여기서는 두 경우만 존재
                if ("updatedAt".equals(property)) {
                    query.orderBy(order.isAscending() ? product.updatedAt.asc() : product.updatedAt.desc());
                } else { // "averageReviewStar"
                    query.orderBy(order.isAscending() ?
                            review.reviewStar.avg().coalesce(0.0).asc() :
                            review.reviewStar.avg().coalesce(0.0).desc());
                }
            }
        }

        List<ProductWithAvgStar> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = Objects.requireNonNullElse(
                queryFactory
                        .select(product.count())
                        .from(like)
                        .join(product).on(like.productId.eq(product.id))
                        .where(like.memberId.eq(memberId)
                                .and(product.deleted.isFalse()))
                        .fetchOne(),
                0L
        );

        return new PageImpl<>(content, pageable, total);
    }
}
