package com.talktrip.talktrip.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talktrip.talktrip.domain.chat.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatMessageResponseDto {

    private final String messageId;
    private final String roomId;
    private final String memberId;
    private final String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime createdAt;

    public static ChatMessageResponseDto from(ChatMessage entity) {
        return new ChatMessageResponseDto(
                entity.getMessageId(),
                entity.getRoomId(),
                entity.getMemberId(),
                entity.getMessage(),
                entity.getCreatedAt()
        );
    }
}