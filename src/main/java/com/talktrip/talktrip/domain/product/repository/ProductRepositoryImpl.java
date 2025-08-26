package com.talktrip.talktrip.domain.product.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.talktrip.talktrip.domain.product.dto.ProductWithAvgStarAndLike;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.QHashTag;
import com.talktrip.talktrip.domain.like.entity.QLike;
import com.talktrip.talktrip.domain.product.entity.QProduct;
import com.talktrip.talktrip.domain.product.entity.QProductOption;
import com.talktrip.talktrip.domain.product.entity.QProductImage;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.review.entity.QReview;
import com.talktrip.talktrip.global.entity.QCountry;
import com.talktrip.talktrip.domain.member.entity.QMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final Set<String> ALLOWED_SORT = Set.of(
            "productName", "price", "discountPrice", "totalStock", "updatedAt"
    );

    private static final String ALL_COUNTRIES = "전체";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DELETED = "DELETED";
    
    private static final QProduct PRODUCT = QProduct.product;
    private static final QCountry COUNTRY = QCountry.country;
    private static final QProductOption PRODUCT_OPTION = QProductOption.productOption;
    private static final QProductImage PRODUCT_IMAGE = QProductImage.productImage;
    private static final QReview REVIEW = QReview.review;
    private static final QHashTag HASH_TAG = QHashTag.hashTag;
    private static final QLike LIKE = QLike.like;
    private static final QMember MEMBER = QMember.member;

    private static BooleanExpression occGoe(Path<String> col, String kwLower, int req) {
        return Expressions.numberTemplate(Integer.class,
                "((length(lower({0})) - length(function('replace', lower({0}), {1}, ''))) / length({1}))",
                col, Expressions.constant(kwLower)
        ).goe(req);
    }

    private BooleanBuilder keywordWhere(List<String> keywords, QProduct p, QCountry c, QHashTag hSub) {
        Map<String, Long> need = keywords.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.toLowerCase(java.util.Locale.ROOT))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        BooleanBuilder where = new BooleanBuilder();
        for (Map.Entry<String, Long> e : need.entrySet()) {
            String kw = e.getKey();
            int req = e.getValue().intValue();

            BooleanExpression perKeyword =
                    occGoe(p.productName, kw, req)
                            .or(occGoe(p.description, kw, req))
                            .or(occGoe(c.name, kw, req))
                            .or(JPAExpressions.selectOne()
                                    .from(hSub)
                                    .where(hSub.product.eq(p)
                                            .and(occGoe(hSub.hashtag, kw, req)))
                                    .exists());

            where.and(perKeyword);
        }
        return where;
    }

    private BooleanExpression hasFutureStock(QProduct p, QProductOption o) {
        return JPAExpressions.selectOne()
                .from(o)
                .where(o.product.eq(p)
                        .and(o.startDate.goe(LocalDate.now()))
                        .and(o.stock.gt(0)))
                .exists();
    }

    private JPQLQuery<Integer> totalStockQuery(QProduct p, QProductOption o) {
        return JPAExpressions
                .select(o.stock.sum().coalesce(0))
                .from(o)
                .where(o.product.eq(p));
    }

    private void applyOrderBySeller(JPAQuery<Product> query, Pageable pageable,
                                    QProduct p, QProductOption o) {
        if (pageable.getSort().isUnsorted()) {
            query.orderBy(p.updatedAt.desc());
            return;
        }

        for (Sort.Order s : pageable.getSort()) {
            String prop = s.getProperty();
            Order dir = s.isDescending() ? Order.DESC : Order.ASC;

            if (!ALLOWED_SORT.contains(prop)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported sort property: " + prop
                );
            }

            switch (prop) {
                case "productName" -> query.orderBy(new OrderSpecifier<>(dir, p.productName));
                case "updatedAt"   -> query.orderBy(new OrderSpecifier<>(dir, p.updatedAt));
                case "totalStock" -> {
                    JPQLQuery<Integer> q = totalStockQuery(p, o);
                    query.orderBy(new OrderSpecifier<>(dir, q));
                }
            }
        }
    }

    @Override
    public Page<Product> findSellerProducts(Long sellerId, String status, String keyword, Pageable pageable) {
        QProduct p = PRODUCT;

        BooleanBuilder where = new BooleanBuilder();
        where.and(p.member.Id.eq(sellerId));

        if (status != null && !status.isBlank()) {
            switch (status.trim().toUpperCase()) {
                case STATUS_ACTIVE -> where.and(p.deleted.isFalse());
                case STATUS_DELETED -> where.and(p.deleted.isTrue());
                case "ALL" -> {}
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported status: " + status
                );
            }
        } else {
            // 기본적으로 삭제되지 않은 상품만 조회
            where.and(p.deleted.isFalse());
        }

        if (keyword != null && !keyword.isBlank()) {
            for (String tok : keyword.trim().split("\\s+")) {
                if (tok.isBlank()) continue;
                where.and(
                        p.productName.containsIgnoreCase(tok)
                                .or(p.description.containsIgnoreCase(tok))
                );
            }
        }

        JPAQuery<Product> dataQuery = queryFactory
                .select(p)
                .from(p)
                .where(where);

        applyOrderBySeller(dataQuery, pageable, p, PRODUCT_OPTION);

        List<Product> content = dataQuery
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(p.id.count())
                .from(p)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Page<ProductWithAvgStarAndLike> searchProductsWithAvgStarAndLike(
            String keyword,
            String countryName,
            LocalDate tomorrow,
            Long memberId,
            Pageable pageable
    ) {
        QProduct p = PRODUCT;
        QCountry c = COUNTRY;
        QProductOption o = PRODUCT_OPTION;
        QReview r = REVIEW;

        BooleanBuilder where = buildSearchWhereClause(p, c, o, keyword, countryName);

        // 메인 쿼리
        JPAQuery<ProductWithAvgStarAndLike> query = buildProductWithAvgStarAndLikeQuery(p, c, o, r, where, memberId);

        // 정렬 적용
        applyOrderByWithProjection(query, pageable, p, o, r);

        List<ProductWithAvgStarAndLike> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회
        Long total = queryFactory
                .select(p.id.countDistinct())
                .from(p)
                .leftJoin(p.country, c)
                .leftJoin(o).on(o.product.eq(p)
                        .and(o.startDate.goe(tomorrow))
                        .and(o.stock.gt(0)))
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private void applyOrderByWithProjection(JPAQuery<ProductWithAvgStarAndLike> query, Pageable pageable, 
                                          QProduct p, QProductOption o, QReview r) {
        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                OrderSpecifier<?> orderSpecifier = switch (order.getProperty()) {
                    case "updatedAt" -> order.getDirection() == Sort.Direction.ASC ? 
                            p.updatedAt.asc() : p.updatedAt.desc();
                    case "productName" -> order.getDirection() == Sort.Direction.ASC ? 
                            p.productName.asc() : p.productName.desc();
                    case "discountPrice" -> order.getDirection() == Sort.Direction.ASC ?
                            o.discountPrice.min().asc() : o.discountPrice.min().desc();
                    case "averageReviewStar" -> order.getDirection() == Sort.Direction.ASC ? 
                            r.reviewStar.avg().coalesce(0.0).asc() : r.reviewStar.avg().coalesce(0.0).desc();
                    default -> p.updatedAt.desc();
                };
                query.orderBy(orderSpecifier);
            }
        } else {
            query.orderBy(p.updatedAt.desc());
        }
    }

    @Override
    public Optional<ProductWithAvgStarAndLike> findByIdWithDetailsAndAvgStarAndLike(
            Long productId,
            LocalDate tomorrow,
            Long memberId
    ) {
        QProduct p = PRODUCT;
        QProductOption o = PRODUCT_OPTION;

        BooleanBuilder where = new BooleanBuilder();
        where.and(p.id.eq(productId));
        where.and(p.deleted.isFalse());
        where.and(hasFutureStock(p, o));

        ProductWithAvgStarAndLike result = buildProductWithAvgStarAndLikeQuery(p, COUNTRY, o, REVIEW, where, memberId)
                .having(o.discountPrice.min().isNotNull())
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<ProductWithAvgStarAndLike> findProductsWithAvgStarAndLikeByIds(
            List<Long> productIds,
            Long memberId,
            Pageable pageable
    ) {
        if (productIds == null || productIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        QProduct p = PRODUCT;
        QCountry c = COUNTRY;
        QProductOption o = PRODUCT_OPTION;
        QReview r = REVIEW;

        BooleanBuilder where = new BooleanBuilder();
        where.and(p.id.in(productIds));
        where.and(p.deleted.isFalse());

        // 메인 쿼리
        JPAQuery<ProductWithAvgStarAndLike> query = buildProductWithAvgStarAndLikeQuery(p, c, o, r, where, memberId);

        // 정렬 적용
        applyOrderByWithProjection(query, pageable, p, o, r);

        List<ProductWithAvgStarAndLike> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회
        Long total = queryFactory
                .select(p.id.countDistinct())
                .from(p)
                .leftJoin(p.country, c)
                .leftJoin(o).on(o.product.eq(p)
                        .and(o.startDate.goe(LocalDate.now()))
                        .and(o.stock.gt(0)))
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // 공통 메서드들
    private BooleanBuilder buildSearchWhereClause(QProduct p, QCountry c, QProductOption o,
                                                  String keyword, String countryName) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(p.deleted.isFalse());

        // 국가 필터
        if (countryName != null && !countryName.isBlank() && !ALL_COUNTRIES.equals(countryName)) {
            where.and(c.name.equalsIgnoreCase(countryName.trim()));
        }

        // 키워드 필터 - 고급 검색 사용
        if (keyword != null && !keyword.isBlank()) {
            List<String> keywords = Arrays.asList(keyword.trim().split("\\s+"));
            where.and(keywordWhere(keywords, p, c, ProductRepositoryImpl.HASH_TAG));
        }

        // 재고 조건
        where.and(hasFutureStock(p, o));

        return where;
    }

    private JPAQuery<ProductWithAvgStarAndLike> buildProductWithAvgStarAndLikeQuery(QProduct p, QCountry c, 
                                                                                   QProductOption o, QReview r, 
                                                                                   BooleanBuilder where, Long memberId) {
        return queryFactory
                .select(Projections.constructor(ProductWithAvgStarAndLike.class,
                        p,
                        r.reviewStar.avg().coalesce(0.0),
                        memberId == null ? Expressions.constant(false) :
                                JPAExpressions.selectOne()
                                        .from(LIKE)
                                        .where(LIKE.productId.eq(p.id)
                                                .and(LIKE.memberId.eq(memberId)))
                                        .exists()
                ))
                .from(p)
                .leftJoin(p.country, c)
                .leftJoin(o).on(o.product.eq(p)
                        .and(o.startDate.goe(LocalDate.now()))
                        .and(o.stock.gt(0)))
                .leftJoin(r).on(r.product.eq(p))
                .where(where)
                .groupBy(p.id, p.productName, p.description, p.thumbnailImageUrl, 
                        p.thumbnailImageHash, p.member, p.country, p.deleted, 
                        p.deletedAt, p.createdAt, p.updatedAt);
    }

    @Override
    public Product findProductWithAllDetailsById(Long productId) {
        QProduct p = PRODUCT;
        QProductOption o = PRODUCT_OPTION;
        QProductImage i = PRODUCT_IMAGE;
        QHashTag h = HASH_TAG;
        QCountry c = COUNTRY;
        QMember m = MEMBER;

        // 메인 쿼리로 Product와 연관 엔티티들을 한 번에 조회
        Product product = queryFactory
                .selectFrom(p)
                .leftJoin(p.country, c).fetchJoin()
                .leftJoin(p.member, m).fetchJoin()
                .where(p.id.eq(productId))
                .fetchOne();

        if (product == null) {
            return null;
        }

        // 연관 엔티티들을 별도로 조회 (MultipleBagFetchException 방지)
        List<ProductOption> options = queryFactory
                .selectFrom(o)
                .where(o.product.eq(product))
                .fetch();

        List<ProductImage> images = queryFactory
                .selectFrom(i)
                .where(i.product.eq(product))
                .orderBy(i.sortOrder.asc())
                .fetch();

        List<HashTag> hashtags = queryFactory
                .selectFrom(h)
                .where(h.product.eq(product))
                .fetch();

        // 연관 엔티티들을 Product에 설정
        product.getProductOptions().addAll(options);
        product.getImages().addAll(images);
        product.getHashtags().addAll(hashtags);

        return product;
    }
}
