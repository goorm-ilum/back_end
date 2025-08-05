package com.talktrip.talktrip.domain.chat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserAlarmStatus is a Querydsl query type for UserAlarmStatus
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserAlarmStatus extends EntityPathBase<UserAlarmStatus> {

    private static final long serialVersionUID = 1839029446L;

    public static final QUserAlarmStatus userAlarmStatus = new QUserAlarmStatus("userAlarmStatus");

    public final NumberPath<Integer> unreadAlarmCount = createNumber("unreadAlarmCount", Integer.class);

    public final StringPath userId = createString("userId");

    public QUserAlarmStatus(String variable) {
        super(UserAlarmStatus.class, forVariable(variable));
    }

    public QUserAlarmStatus(Path<? extends UserAlarmStatus> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserAlarmStatus(PathMetadata metadata) {
        super(UserAlarmStatus.class, metadata);
    }

}

