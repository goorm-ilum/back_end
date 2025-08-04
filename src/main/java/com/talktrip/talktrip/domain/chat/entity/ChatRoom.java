package com.talktrip.talktrip.domain.chat.entity;

import com.talktrip.talktrip.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "chating_room_tab")
public class ChatRoom extends BaseEntity {
    @Id
    @Column(name = "room_id", length = 10)
    private String roomId;
    private String lastMessageId;
    private String lastMessage;
    private String title;
    private int notReadMessageCount;

}
