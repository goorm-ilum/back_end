package com.talktrip.talktrip.domain.buyer.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBuyer is a Querydsl query type for Buyer
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBuyer extends EntityPathBase<Buyer> {

    private static final long serialVersionUID = 268998656L;

    public static final QBuyer buyer = new QBuyer("buyer");

    public final com.talktrip.talktrip.global.entity.QBaseEntity _super = new com.talktrip.talktrip.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath nickname = createString("nickname");

    public final StringPath role = createString("role");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QBuyer(String variable) {
        super(Buyer.class, forVariable(variable));
    }

    public QBuyer(Path<? extends Buyer> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBuyer(PathMetadata metadata) {
        super(Buyer.class, metadata);
    }

}

