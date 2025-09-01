package com.talktrip.talktrip.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talktrip.talktrip.domain.chat.entity.RoomType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChatRoomDTO {
    private String roomId;
//    private String roomAccountId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    private String title;
    private String lastMessage;
    private Long notReadMessageCount;
    private RoomType roomType;  // RoomType 필드 추가

    public ChatRoomDTO(
            String roomId,
            String roomAccountId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String title,
            String lastMessage,
            Long notReadMessageCount,
            RoomType roomType  // RoomType 생성자 매개변수 추가
    ) {
        this.roomId = roomId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.title = title;
        this.lastMessage = lastMessage;
        this.notReadMessageCount = notReadMessageCount;
        this.roomType = roomType;  // 값 초기화
    }
}
