package com.talktrip.talktrip.domain.chat.dto.request;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue
    private Long id;

    private String roomId;
    private String senderId;
    private String message;
    private LocalDateTime createdAt;

    public ChatMessage(String roomId, String senderId, String message, LocalDateTime createdAt) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.message = message;
        this.createdAt = createdAt;
    }
}
