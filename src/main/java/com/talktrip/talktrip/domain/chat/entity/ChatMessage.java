package com.talktrip.talktrip.domain.chat.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "chatting_message_history_tab")
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {

    @Id
    private String messageId; // ← Long → String 으로 변경

    private String roomId;

    private String accountEmail;

    private String message;

    @CreatedDate
    private LocalDateTime createdAt;
    public ChatMessage(String messageId, String roomId, String accountEmail, String message, LocalDateTime createdAt) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.accountEmail = accountEmail;
        this.message = message;
        this.createdAt = createdAt;
    }


}