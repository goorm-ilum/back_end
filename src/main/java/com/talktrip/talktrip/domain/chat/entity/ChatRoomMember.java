package com.talktrip.talktrip.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chating_room_member_tab")
public class ChatRoomMember {

    @Id
    private String roomMemberId;

    @Column(name = "room_id")
    private String roomId;

    @Column(name = "member_id")
    private String memberId;

    @Column(name = "last_read_message_id")
    private String lastReadMessageId;

    @CreationTimestamp
    @Column(name = "last_member_read_time")
    private LocalDateTime lastMemberReadTime;

}
