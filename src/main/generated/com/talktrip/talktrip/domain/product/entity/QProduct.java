package com.talktrip.talktrip.domain.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProduct is a Querydsl query type for Product
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProduct extends EntityPathBase<Product> {

    private static final long serialVersionUID = -637131264L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProduct product = new QProduct("product");

    public final com.talktrip.talktrip.global.entity.QBaseEntity _super = new com.talktrip.talktrip.global.entity.QBaseEntity(this);

    public final com.talktrip.talktrip.global.entity.QCountry country;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final BooleanPath deleted = createBoolean("deleted");

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final ListPath<HashTag, QHashTag> hashtags = this.<HashTag, QHashTag>createList("hashtags", HashTag.class, QHashTag.class, PathInits.DIRECT2);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<ProductImage, QProductImage> images = this.<ProductImage, QProductImage>createList("images", ProductImage.class, QProductImage.class, PathInits.DIRECT2);

    public final com.talktrip.talktrip.domain.member.entity.QMember member;

    public final StringPath productName = createString("productName");

    public final ListPath<ProductOption, QProductOption> productOptions = this.<ProductOption, QProductOption>createList("productOptions", ProductOption.class, QProductOption.class, PathInits.DIRECT2);

    public final ListPath<com.talktrip.talktrip.domain.review.entity.Review, com.talktrip.talktrip.domain.review.entity.QReview> reviews = this.<com.talktrip.talktrip.domain.review.entity.Review, com.talktrip.talktrip.domain.review.entity.QReview>createList("reviews", com.talktrip.talktrip.domain.review.entity.Review.class, com.talktrip.talktrip.domain.review.entity.QReview.class, PathInits.DIRECT2);

    public final StringPath thumbnailImageHash = createString("thumbnailImageHash");

    public final StringPath thumbnailImageUrl = createString("thumbnailImageUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QProduct(String variable) {
        this(Product.class, forVariable(variable), INITS);
    }

    public QProduct(Path<? extends Product> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProduct(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProduct(PathMetadata metadata, PathInits inits) {
        this(Product.class, metadata, inits);
    }

    public QProduct(Class<? extends Product> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.country = inits.isInitialized("country") ? new com.talktrip.talktrip.global.entity.QCountry(forProperty("country")) : null;
        this.member = inits.isInitialized("member") ? new com.talktrip.talktrip.domain.member.entity.QMember(forProperty("member")) : null;
    }

}

