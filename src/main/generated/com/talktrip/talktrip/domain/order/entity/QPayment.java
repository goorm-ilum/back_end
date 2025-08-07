package com.talktrip.talktrip.domain.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPayment is a Querydsl query type for Payment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPayment extends EntityPathBase<Payment> {

    private static final long serialVersionUID = 754817560L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPayment payment = new QPayment("payment");

    public final StringPath accountBank = createString("accountBank");

    public final DateTimePath<java.time.LocalDateTime> approvedAt = createDateTime("approvedAt", java.time.LocalDateTime.class);

    public final StringPath cardCompany = createString("cardCompany");

    public final QCardPayment cardPayment;

    public final StringPath easyPayProvider = createString("easyPayProvider");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isPartialCancelable = createBoolean("isPartialCancelable");

    public final EnumPath<com.talktrip.talktrip.domain.order.enums.PaymentMethod> method = createEnum("method", com.talktrip.talktrip.domain.order.enums.PaymentMethod.class);

    public final QOrder order;

    public final StringPath paymentKey = createString("paymentKey");

    public final EnumPath<com.talktrip.talktrip.domain.order.enums.PaymentProvider> provider = createEnum("provider", com.talktrip.talktrip.domain.order.enums.PaymentProvider.class);

    public final StringPath receiptUrl = createString("receiptUrl");

    public final StringPath status = createString("status");

    public final NumberPath<Integer> suppliedAmount = createNumber("suppliedAmount", Integer.class);

    public final NumberPath<Integer> totalAmount = createNumber("totalAmount", Integer.class);

    public final NumberPath<Integer> vat = createNumber("vat", Integer.class);

    public QPayment(String variable) {
        this(Payment.class, forVariable(variable), INITS);
    }

    public QPayment(Path<? extends Payment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPayment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPayment(PathMetadata metadata, PathInits inits) {
        this(Payment.class, metadata, inits);
    }

    public QPayment(Class<? extends Payment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.cardPayment = inits.isInitialized("cardPayment") ? new QCardPayment(forProperty("cardPayment"), inits.get("cardPayment")) : null;
        this.order = inits.isInitialized("order") ? new QOrder(forProperty("order"), inits.get("order")) : null;
    }

}

