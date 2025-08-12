package com.talktrip.talktrip.domain.chat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatRoomAccount is a Querydsl query type for ChatRoomAccount
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatRoomAccount extends EntityPathBase<ChatRoomAccount> {

    private static final long serialVersionUID = -1808401848L;

    public static final QChatRoomAccount chatRoomAccount = new QChatRoomAccount("chatRoomAccount");

    public final StringPath accountEmail = createString("accountEmail");

    public final NumberPath<Integer> isDel = createNumber("isDel", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> lastMemberReadTime = createDateTime("lastMemberReadTime", java.time.LocalDateTime.class);

    public final StringPath roomAccountId = createString("roomAccountId");

    public final StringPath roomId = createString("roomId");

    public QChatRoomAccount(String variable) {
        super(ChatRoomAccount.class, forVariable(variable));
    }

    public QChatRoomAccount(Path<? extends ChatRoomAccount> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatRoomAccount(PathMetadata metadata) {
        super(ChatRoomAccount.class, metadata);
    }

}

