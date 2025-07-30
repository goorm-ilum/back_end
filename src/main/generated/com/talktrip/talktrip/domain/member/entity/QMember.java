package com.talktrip.talktrip.domain.member.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMember is a Querydsl query type for Member
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMember extends EntityPathBase<Member> {

    private static final long serialVersionUID = 2083125102L;

    public static final QMember member = new QMember("member1");

    public final com.talktrip.talktrip.global.entity.QBaseEntity _super = new com.talktrip.talktrip.global.entity.QBaseEntity(this);

    public final StringPath accountEmail = createString("accountEmail");

    public final DatePath<java.time.LocalDate> birthday = createDate("birthday", java.time.LocalDate.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<com.talktrip.talktrip.domain.member.enums.Gender> gender = createEnum("gender", com.talktrip.talktrip.domain.member.enums.Gender.class);

    public final NumberPath<Long> Id = createNumber("Id", Long.class);

    public final StringPath name = createString("name");

    public final StringPath nickname = createString("nickname");

    public final StringPath phoneNum = createString("phoneNum");

    public final ListPath<com.talktrip.talktrip.domain.product.entity.Product, com.talktrip.talktrip.domain.product.entity.QProduct> products = this.<com.talktrip.talktrip.domain.product.entity.Product, com.talktrip.talktrip.domain.product.entity.QProduct>createList("products", com.talktrip.talktrip.domain.product.entity.Product.class, com.talktrip.talktrip.domain.product.entity.QProduct.class, PathInits.DIRECT2);

    public final StringPath profileImage = createString("profileImage");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final EnumPath<com.talktrip.talktrip.domain.member.enums.UserRole> userRole = createEnum("userRole", com.talktrip.talktrip.domain.member.enums.UserRole.class);

    public final EnumPath<com.talktrip.talktrip.domain.member.enums.UserState> userState = createEnum("userState", com.talktrip.talktrip.domain.member.enums.UserState.class);

    public QMember(String variable) {
        super(Member.class, forVariable(variable));
    }

    public QMember(Path<? extends Member> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMember(PathMetadata metadata) {
        super(Member.class, metadata);
    }

}

