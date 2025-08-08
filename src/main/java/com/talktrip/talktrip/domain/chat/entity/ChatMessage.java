package com.talktrip.talktrip.domain.chat.entity;

import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chating_message_history_tab")
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)

public class ChatMessage {

    @Id
    private String messageId; // ← Long → String 으로 변경

    private String roomId;

    private String memberId;

    private String message;

    @CreatedDate
    private LocalDateTime createdAt;
    public ChatMessage(String messageId, String roomId, String memberId, String message, LocalDateTime createdAt) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.memberId = memberId;
        this.message = message;
        this.createdAt = createdAt;
    }


}