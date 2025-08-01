package com.talktrip.talktrip.domain.chat.dto.request;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private String roomId;
    private String senderId;
    private String message;

    public static ChatMessageDto fromEntity(ChatMessage entity) {
        return new ChatMessageDto(
                entity.getRoomId(),
                entity.getSenderId(),
                entity.getMessage()
        );
    }
}