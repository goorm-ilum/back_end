package com.talktrip.talktrip.domain.seller.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSeller is a Querydsl query type for Seller
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSeller extends EntityPathBase<Seller> {

    private static final long serialVersionUID = 1447201912L;

    public static final QSeller seller = new QSeller("seller");

    public final com.talktrip.talktrip.global.entity.QBaseEntity _super = new com.talktrip.talktrip.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath nickname = createString("nickname");

    public final ListPath<com.talktrip.talktrip.domain.product.entity.Product, com.talktrip.talktrip.domain.product.entity.QProduct> products = this.<com.talktrip.talktrip.domain.product.entity.Product, com.talktrip.talktrip.domain.product.entity.QProduct>createList("products", com.talktrip.talktrip.domain.product.entity.Product.class, com.talktrip.talktrip.domain.product.entity.QProduct.class, PathInits.DIRECT2);

    public final StringPath role = createString("role");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QSeller(String variable) {
        super(Seller.class, forVariable(variable));
    }

    public QSeller(Path<? extends Seller> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSeller(PathMetadata metadata) {
        super(Seller.class, metadata);
    }

}

