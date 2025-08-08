package com.talktrip.talktrip.domain.chat.entity;


import com.talktrip.talktrip.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@Table(name = "chating_room_member_tab")
public class ChatRoomMember {
    
    @Id
    @Column(name = "room_member_id")
    private String roomMemberId;

    @Column(name = "member_id")
    private String memberId;
    
    @Column(name = "room_id")
    private String roomId;
    
    @Column(name = "last_member_read_time")
    private LocalDateTime lastMemberReadTime;
    @Column(name = "is_del")
    private int isDel;

    @Builder
    public ChatRoomMember(String roomMemberId,String memberId, String roomId) {
        this.roomMemberId = roomMemberId;
        this.memberId = memberId;
        this.roomId = roomId;
    }
    
    public void updateLastReadTime(LocalDateTime time) {
        this.lastMemberReadTime = time;
    }

    public static ChatRoomMember create(String roomId, String memberId) {
        String newRoomMemberId = "RM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return ChatRoomMember.builder()
                .roomMemberId(newRoomMemberId)
                .roomId(roomId)
                .memberId(memberId)
                .build();
    }
}