package com.talktrip.talktrip.domain.chat.entity;

import com.talktrip.talktrip.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@NoArgsConstructor
@Table(name = "chating_message_history_tab")
public class ChatMessageHistory extends BaseEntity {

    @Id
    private String messageId;
    @Column(name = "room_id", length = 10)
    private String roomId;
    private String senderId;
    private String message;
    private String memberId;

    public ChatMessageHistory(String messageId,String roomId, String senderId, String message, LocalDateTime createdAt) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.senderId = senderId;
        this.message = message;

    }
}
