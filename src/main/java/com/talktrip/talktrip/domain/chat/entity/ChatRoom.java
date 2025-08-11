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
@Table(name = "chatting_room_tab")
public class ChatRoom extends BaseEntity {
    @Id
    private String roomId;
    private String roomAccountId;
    private String title;
    private String lastMessage;
    private int notReadMessageCount;

}
