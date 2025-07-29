package com.talktrip.talktrip.domain.product.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.QHashTag;
import com.talktrip.talktrip.domain.product.entity.QProduct;
import com.talktrip.talktrip.global.entity.QCountry;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Product> searchByKeywords(List<String> keywords, int offset, int limit) {
        QProduct product = QProduct.product;
        QHashTag hashtag = QHashTag.hashTag;
        QCountry country = QCountry.country;

        BooleanBuilder builder = new BooleanBuilder();

        for (String keyword : keywords) {
            String like = "%" + keyword + "%";
            BooleanBuilder perKeyword = new BooleanBuilder();
            perKeyword.or(product.productName.likeIgnoreCase(like))
                    .or(product.description.likeIgnoreCase(like))
                    .or(hashtag.hashtag.likeIgnoreCase(like))
                    .or(country.name.likeIgnoreCase(like));
            builder.and(perKeyword);
        }

        return queryFactory.selectDistinct(product)
                .from(product)
                .leftJoin(product.hashtags, hashtag).fetchJoin()
                .leftJoin(product.country, country).fetchJoin()
                .where(builder)
                .offset(offset)
                .limit(limit)
                .fetch();
    }
}
