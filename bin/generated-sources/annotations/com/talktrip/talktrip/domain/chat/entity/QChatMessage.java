package com.talktrip.talktrip.domain.chat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatMessage is a Querydsl query type for ChatMessage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatMessage extends EntityPathBase<ChatMessage> {

    private static final long serialVersionUID = -1075175299L;

    public static final QChatMessage chatMessage = new QChatMessage("chatMessage");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath memberId = createString("memberId");

    public final StringPath message = createString("message");

    public final StringPath messageId = createString("messageId");

    public final StringPath roomId = createString("roomId");

    public QChatMessage(String variable) {
        super(ChatMessage.class, forVariable(variable));
    }

    public QChatMessage(Path<? extends ChatMessage> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatMessage(PathMetadata metadata) {
        super(ChatMessage.class, metadata);
    }

}

