package com.talktrip.talktrip.domain.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCardPayment is a Querydsl query type for CardPayment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCardPayment extends EntityPathBase<CardPayment> {

    private static final long serialVersionUID = 821283816L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCardPayment cardPayment = new QCardPayment("cardPayment");

    public final StringPath acquirerCode = createString("acquirerCode");

    public final StringPath acquireStatus = createString("acquireStatus");

    public final NumberPath<Integer> amount = createNumber("amount", Integer.class);

    public final StringPath approveNo = createString("approveNo");

    public final StringPath cardNumber = createString("cardNumber");

    public final StringPath cardType = createString("cardType");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> installmentMonths = createNumber("installmentMonths", Integer.class);

    public final BooleanPath isInterestFree = createBoolean("isInterestFree");

    public final StringPath issuerCode = createString("issuerCode");

    public final StringPath ownerType = createString("ownerType");

    public final QPayment payment;

    public QCardPayment(String variable) {
        this(CardPayment.class, forVariable(variable), INITS);
    }

    public QCardPayment(Path<? extends CardPayment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCardPayment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCardPayment(PathMetadata metadata, PathInits inits) {
        this(CardPayment.class, metadata, inits);
    }

    public QCardPayment(Class<? extends CardPayment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.payment = inits.isInitialized("payment") ? new QPayment(forProperty("payment"), inits.get("payment")) : null;
    }

}

