package com.talktrip.talktrip.domain.chat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatRoomUser is a Querydsl query type for ChatRoomUser
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatRoomUser extends EntityPathBase<ChatRoomUser> {

    private static final long serialVersionUID = 1318119824L;

    public static final QChatRoomUser chatRoomUser = new QChatRoomUser("chatRoomUser");

    public final StringPath lastReadMessageId = createString("lastReadMessageId");

    public final StringPath memberId = createString("memberId");

    public final StringPath roomId = createString("roomId");

    public final StringPath roomMemberId = createString("roomMemberId");

    public QChatRoomUser(String variable) {
        super(ChatRoomUser.class, forVariable(variable));
    }

    public QChatRoomUser(Path<? extends ChatRoomUser> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatRoomUser(PathMetadata metadata) {
        super(ChatRoomUser.class, metadata);
    }

}

