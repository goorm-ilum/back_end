package com.talktrip.talktrip.domain.chat.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChatRoomDTO {
    private String roomId;
    private String roomAccountId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String title;
    private String lastMessage;
    private Long notReadMessageCount;

    public ChatRoomDTO(
            String roomId,
            String roomAccountId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String title,
            String lastMessage,
            Long notReadMessageCount
    ) {
        this.roomId = roomId;
        this.roomAccountId = roomAccountId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.title = title;
        this.lastMessage = lastMessage;
        this.notReadMessageCount = notReadMessageCount;
    }
}
