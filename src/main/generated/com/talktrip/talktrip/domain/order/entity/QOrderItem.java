package com.talktrip.talktrip.domain.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderItem is a Querydsl query type for OrderItem
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrderItem extends EntityPathBase<OrderItem> {

    private static final long serialVersionUID = -775173357L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrderItem orderItem = new QOrderItem("orderItem");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QOrder order;

    public final NumberPath<Integer> price = createNumber("price", Integer.class);

    public final com.talktrip.talktrip.domain.product.entity.QProduct product;

    public final com.talktrip.talktrip.domain.product.entity.QProductOption productOption;

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public QOrderItem(String variable) {
        this(OrderItem.class, forVariable(variable), INITS);
    }

    public QOrderItem(Path<? extends OrderItem> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrderItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrderItem(PathMetadata metadata, PathInits inits) {
        this(OrderItem.class, metadata, inits);
    }

    public QOrderItem(Class<? extends OrderItem> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.order = inits.isInitialized("order") ? new QOrder(forProperty("order"), inits.get("order")) : null;
        this.product = inits.isInitialized("product") ? new com.talktrip.talktrip.domain.product.entity.QProduct(forProperty("product"), inits.get("product")) : null;
        this.productOption = inits.isInitialized("productOption") ? new com.talktrip.talktrip.domain.product.entity.QProductOption(forProperty("productOption"), inits.get("productOption")) : null;
    }

}

