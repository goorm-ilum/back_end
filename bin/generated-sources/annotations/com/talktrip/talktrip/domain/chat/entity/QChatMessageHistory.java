package com.talktrip.talktrip.domain.chat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatMessageHistory is a Querydsl query type for ChatMessageHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatMessageHistory extends EntityPathBase<ChatMessageHistory> {

    private static final long serialVersionUID = -572701353L;

    public static final QChatMessageHistory chatMessageHistory = new QChatMessageHistory("chatMessageHistory");

    public final com.talktrip.talktrip.global.entity.QBaseEntity _super = new com.talktrip.talktrip.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath memberId = createString("memberId");

    public final StringPath message = createString("message");

    public final StringPath messageId = createString("messageId");

    public final StringPath roomId = createString("roomId");

    public final StringPath senderId = createString("senderId");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QChatMessageHistory(String variable) {
        super(ChatMessageHistory.class, forVariable(variable));
    }

    public QChatMessageHistory(Path<? extends ChatMessageHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatMessageHistory(PathMetadata metadata) {
        super(ChatMessageHistory.class, metadata);
    }

}

