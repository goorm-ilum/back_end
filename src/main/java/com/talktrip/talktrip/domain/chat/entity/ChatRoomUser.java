package com.talktrip.talktrip.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "chating_room_users_tab")
public class ChatRoomUser {

    @Id
    private String roomMemberId;

    @Column(name = "room_id")
    private String roomId;

    @Column(name = "member_id")
    private String memberId;

    @Column(name = "last_read_message_id")
    private String lastReadMessageId;

    // 연관관계, getter/setter 생략
}
