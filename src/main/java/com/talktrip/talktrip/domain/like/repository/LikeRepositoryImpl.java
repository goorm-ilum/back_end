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

import java.util.List;

import static com.talktrip.talktrip.domain.like.entity.QLike.like;
import static com.talktrip.talktrip.domain.product.entity.QProduct.product;
import static com.talktrip.talktrip.domain.review.entity.QReview.review;

@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

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
        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                String property = order.getProperty();
                switch (property) {
                    case "updatedAt":
                        if (order.isAscending()) {
                            query.orderBy(product.updatedAt.asc());
                        } else {
                            query.orderBy(product.updatedAt.desc());
                        }
                        break;
                    case "productName":
                        if (order.isAscending()) {
                            query.orderBy(product.productName.asc());
                        } else {
                            query.orderBy(product.productName.desc());
                        }
                        break;
                    default:
                        query.orderBy(product.updatedAt.desc());
                        break;
                }
            }
        } else {
            query.orderBy(product.updatedAt.desc());
        }

        List<ProductWithAvgStar> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(product.count())
                .from(like)
                .join(product).on(like.productId.eq(product.id))
                .where(like.memberId.eq(memberId)
                        .and(product.deleted.eq(false)))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
