package com.talktrip.talktrip.domain.chat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatRoomMember is a Querydsl query type for ChatRoomMember
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatRoomMember extends EntityPathBase<ChatRoomMember> {

    private static final long serialVersionUID = -543937761L;

    public static final QChatRoomMember chatRoomMember = new QChatRoomMember("chatRoomMember");

    public final DateTimePath<java.time.LocalDateTime> lastMemberReadTime = createDateTime("lastMemberReadTime", java.time.LocalDateTime.class);

    public final StringPath memberId = createString("memberId");

    public final StringPath roomId = createString("roomId");

    public final StringPath roomMemberId = createString("roomMemberId");

    public QChatRoomMember(String variable) {
        super(ChatRoomMember.class, forVariable(variable));
    }

    public QChatRoomMember(Path<? extends ChatRoomMember> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatRoomMember(PathMetadata metadata) {
        super(ChatRoomMember.class, metadata);
    }

}

