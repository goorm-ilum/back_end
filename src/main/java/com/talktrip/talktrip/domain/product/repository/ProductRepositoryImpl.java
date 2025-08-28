package com.talktrip.talktrip.domain.product.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.talktrip.talktrip.domain.like.entity.QLike;
import com.talktrip.talktrip.domain.member.entity.QMember;
import com.talktrip.talktrip.domain.product.dto.ProductWithAvgStarAndLike;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductImage;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.entity.QHashTag;
import com.talktrip.talktrip.domain.product.entity.QProduct;
import com.talktrip.talktrip.domain.product.entity.QProductImage;
import com.talktrip.talktrip.domain.product.entity.QProductOption;
import com.talktrip.talktrip.domain.review.entity.QReview;
import com.talktrip.talktrip.global.entity.QCountry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final Set<String> ALLOWED_SELLER_SORT = Set.of(
            "productName", "updatedAt", "totalStock"
    );

    private static final Set<String> ALLOWED_SEARCH_SORT = Set.of(
            "updatedAt", "productName", "discountPrice", "averageReviewStar"
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

    private static final Pattern WS = Pattern.compile("\\s+");

    private static List<String> tokensOf(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        return WS.splitAsStream(keyword.trim())
                .toList();
    }

    private static List<String> distinctTokensLower(List<String> tokens) {
        return tokens.stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static Map<String, Integer> tokenCountsLower(List<String> tokens) {
        Map<String, Integer> freq = new HashMap<>();
        for (String s : tokens) {
            String k = s.toLowerCase(Locale.ROOT);
            freq.put(k, freq.getOrDefault(k, 0) + 1);
        }
        return freq;
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "";
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private BooleanBuilder keywordFilter(List<String> rawTokens, QProduct p, QCountry c, QHashTag h) {
        List<String> distinct = distinctTokensLower(rawTokens);
        Map<String, Integer> need = tokenCountsLower(rawTokens);

        BooleanExpression nameAll = Expressions.TRUE;
        for (String kw : distinct) {
            nameAll = nameAll.and(p.productName.containsIgnoreCase(kw));
        }

        BooleanExpression descAll = Expressions.TRUE;
        for (String kw : distinct) {
            descAll = descAll.and(p.description.containsIgnoreCase(kw));
        }

        BooleanExpression countryAny = Expressions.FALSE;
        for (String kw : distinct) {
            countryAny = countryAny.or(c.name.containsIgnoreCase(kw));
        }

        BooleanExpression tagsAll = Expressions.TRUE;
        for (Map.Entry<String, Integer> e : need.entrySet()) {
            String kw = e.getKey();
            int req = e.getValue();
            JPQLQuery<Long> countQuery = JPAExpressions
                    .select(h.id.count())
                    .from(h)
                    .where(h.product.eq(p)
                            .and(h.hashtag.containsIgnoreCase(kw)));
            tagsAll = tagsAll.and(countQuery.goe((long) req));
        }

        BooleanBuilder orGroup = new BooleanBuilder();
        orGroup.or(nameAll).or(descAll).or(countryAny).or(tagsAll);
        return orGroup;
    }

    private BooleanExpression hasFutureStock(QProduct p, LocalDate tomorrow) {
        QProductOption oSub = new QProductOption("productOptionFutureStock");
        return JPAExpressions.selectOne()
                .from(oSub)
                .where(oSub.product.eq(p)
                        .and(oSub.startDate.goe(tomorrow))
                        .and(oSub.stock.gt(0)))
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
            switch (prop) {
                case "productName" -> query.orderBy(new OrderSpecifier<>(dir, p.productName));
                case "updatedAt" -> query.orderBy(new OrderSpecifier<>(dir, p.updatedAt));
                case "totalStock" -> {
                    JPQLQuery<Integer> q = totalStockQuery(p, o);
                    query.orderBy(new OrderSpecifier<>(dir, q));
                }
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unsupported sort property: " + prop
                );
            }
        }
    }

    @Override
    public Page<Product> findSellerProducts(Long sellerId, String status, String keyword, Pageable pageable) {
        QProduct p = PRODUCT;

        BooleanBuilder where = new BooleanBuilder();
        where.and(p.member.Id.eq(sellerId));

        String st = normalizeStatus(status);
        switch (st) {
            case STATUS_ACTIVE -> where.and(p.deleted.isFalse());
            case STATUS_DELETED -> where.and(p.deleted.isTrue());
            case "ALL" -> {}
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported status: " + status
            );
        }

        List<String> tokens = tokensOf(keyword);
        where.and(keywordFilter(tokens, p, COUNTRY, HASH_TAG));

        JPAQuery<Product> dataQuery = queryFactory
                .select(p)
                .from(p)
                .where(where);

        applyOrderBySeller(dataQuery, pageable, p, PRODUCT_OPTION);

        List<Product> content = dataQuery
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = Objects.requireNonNullElse(
                queryFactory
                        .select(p.id.count())
                        .from(p)
                        .where(where)
                        .fetchOne(),
                0L
        );

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<ProductWithAvgStarAndLike> searchProductsWithAvgStarAndLike(
            String keyword,
            String countryName,
            Long memberId,
            Pageable pageable
    ) {
        QProduct p = PRODUCT;
        QCountry c = COUNTRY;
        QProductOption o = PRODUCT_OPTION;
        QReview r = REVIEW;

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        BooleanBuilder where = new BooleanBuilder();
        where.and(p.deleted.isFalse());

        String cn = Optional.ofNullable(countryName).map(String::trim).orElse("");
        BooleanExpression countryExpr =
                (cn.isEmpty() || ALL_COUNTRIES.equals(cn)) ? Expressions.TRUE
                        : c.name.equalsIgnoreCase(cn);
        where.and(countryExpr);

        List<String> tokens = tokensOf(keyword);
        where.and(keywordFilter(tokens, p, c, HASH_TAG));

        where.and(hasFutureStock(p, tomorrow));

        JPAQuery<ProductWithAvgStarAndLike> query = buildProductWithAvgStarAndLikeQuery(
                p, c, o, r, where, memberId, tomorrow
        );

        applyOrderByWithProjection(query, pageable, p, o, r);

        List<ProductWithAvgStarAndLike> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = java.util.Objects.requireNonNullElse(
                queryFactory
                        .select(p.id.countDistinct().coalesce(0L))
                        .from(p)
                        .leftJoin(p.country, c)
                        .leftJoin(o).on(o.product.eq(p)
                                .and(o.startDate.goe(tomorrow))
                                .and(o.stock.gt(0)))
                        .where(where)
                        .fetchOne(),
                0L
        );

        return new PageImpl<>(content, pageable, total);

    }

    private void applyOrderByWithProjection(JPAQuery<ProductWithAvgStarAndLike> query, Pageable pageable,
                                            QProduct p, QProductOption o, QReview r) {
        if (pageable.getSort().isUnsorted()) {
            query.orderBy(p.updatedAt.desc());
            return;
        }
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            switch (property) {
                case "updatedAt" ->
                        query.orderBy(order.getDirection() == Sort.Direction.ASC ? p.updatedAt.asc() : p.updatedAt.desc());
                case "productName" ->
                        query.orderBy(order.getDirection() == Sort.Direction.ASC ? p.productName.asc() : p.productName.desc());
                case "discountPrice" ->
                        query.orderBy(order.getDirection() == Sort.Direction.ASC ? o.discountPrice.min().asc() : o.discountPrice.min().desc());
                case "averageReviewStar" ->
                        query.orderBy(order.getDirection() == Sort.Direction.ASC ? r.reviewStar.avg().coalesce(0.0).asc() : r.reviewStar.avg().coalesce(0.0).desc());
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unsupported sort property: " + property
                );
            }
        }
    }

    @Override
    public Optional<ProductWithAvgStarAndLike> findByIdWithDetailsAndAvgStarAndLike(
            Long productId,
            Long memberId
    ) {
        QProduct p = PRODUCT;
        QProductOption o = PRODUCT_OPTION;

        BooleanBuilder where = new BooleanBuilder();
        where.and(p.id.eq(productId));
        where.and(p.deleted.isFalse());
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        where.and(hasFutureStock(p, tomorrow));

        ProductWithAvgStarAndLike result = buildProductWithAvgStarAndLikeQuery(
                p, COUNTRY, o, REVIEW, where, memberId, tomorrow
        )
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

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        JPAQuery<ProductWithAvgStarAndLike> query = buildProductWithAvgStarAndLikeQuery(
                p, c, o, r, where, memberId, tomorrow
        );

        applyOrderByWithProjection(query, pageable, p, o, r);

        List<ProductWithAvgStarAndLike> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = java.util.Objects.requireNonNullElse(
                queryFactory
                        .select(p.id.countDistinct().coalesce(0L))
                        .from(p)
                        .leftJoin(p.country, c)
                        .leftJoin(o).on(o.product.eq(p)
                                .and(o.startDate.goe(tomorrow))
                                .and(o.stock.gt(0)))
                        .where(where)
                        .fetchOne(),
                0L
        );

        return new PageImpl<>(content, pageable, total);

    }

    private JPAQuery<ProductWithAvgStarAndLike> buildProductWithAvgStarAndLikeQuery(QProduct p, QCountry c,
                                                                                    QProductOption o, QReview r,
                                                                                    BooleanBuilder where, Long memberId,
                                                                                    LocalDate tomorrow) {
        return queryFactory
                .select(com.querydsl.core.types.Projections.constructor(ProductWithAvgStarAndLike.class,
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
                        .and(o.startDate.goe(tomorrow))
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

        Product product = queryFactory
                .selectFrom(p)
                .leftJoin(p.country, c).fetchJoin()
                .leftJoin(p.member, m).fetchJoin()
                .where(p.id.eq(productId))
                .fetchOne();

        if (product == null) {
            return null;
        }

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

        product.getProductOptions().addAll(options);
        product.getImages().addAll(images);
        product.getHashtags().addAll(hashtags);

        return product;
    }
}
