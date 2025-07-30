package com.talktrip.talktrip.domain.member.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMember is a Querydsl query type for Member
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMember extends EntityPathBase<Member> {

    private static final long serialVersionUID = 2083125102L;

    public static final QMember member = new QMember("member1");

    public final StringPath accountEmail = createString("accountEmail");

    public final DatePath<java.time.LocalDate> birthday = createDate("birthday", java.time.LocalDate.class);

    public final EnumPath<com.talktrip.talktrip.domain.member.enums.Gender> gender = createEnum("gender", com.talktrip.talktrip.domain.member.enums.Gender.class);

    public final StringPath name = createString("name");

    public final StringPath nickname = createString("nickname");

    public final StringPath phoneNum = createString("phoneNum");

    public final StringPath profileImage = createString("profileImage");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

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

