package com.talktrip.talktrip.domain.product.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.QHashTag;
import com.talktrip.talktrip.domain.product.entity.QProduct;
import com.talktrip.talktrip.global.entity.QCountry;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // length(lower(col))과 replace를 이용해 부분문자열 등장 횟수 >= req 검사
    private static BooleanExpression occGoe(Path<String> col, String kwLower, int req) {
        // Hibernate 6/HQL 안전 버전: function('replace', ...)
        return Expressions.numberTemplate(Integer.class,
                "((length(lower({0})) - length(function('replace', lower({0}), {1}, ''))) / length({1}))",
                col, Expressions.constant(kwLower)
        ).goe(req);
    }

    @Override
    public List<Product> searchByKeywords(List<String> keywords, String countryName, int offset, int limit) {
        QProduct p = QProduct.product;
        QCountry c = QCountry.country;
        QHashTag hSub = new QHashTag("hSub"); // 해시태그 서브쿼리 전용 별칭

        // 1) 키워드 요구 횟수 맵 (소문자 통일)
        Map<String, Long> need = keywords.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        BooleanBuilder where = new BooleanBuilder();

        // 2) 키워드별 조건(AND): 이름/설명/국가 중 하나에서 req회 이상 OR
        //    또는 "이 상품의 어떤 해시태그든" req회 이상 포함(EXISTS)
        for (Map.Entry<String, Long> e : need.entrySet()) {
            String kw = e.getKey();              // 이미 소문자
            int req = e.getValue().intValue();   // 요구 횟수

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

        // 3) 국가 필터: '전체'가 아니면 AND 추가
        if (countryName != null) {
            String cn = countryName.trim();
            if (!cn.isEmpty() && !"전체".equals(cn)) {
                where.and(c.name.equalsIgnoreCase(cn));
            }
        }

        // 4) 조회: 해시태그는 EXISTS로 검사했으므로 fetchJoin 불필요
        return queryFactory
                .selectDistinct(p)         // 컬렉션 조인 중복 방지
                .from(p)
                .leftJoin(p.country, c).fetchJoin()
                .where(where)
                .offset(offset)
                .limit(limit)
                .fetch();
    }
}
