package com.talktrip.talktrip.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talktrip.talktrip.domain.chat.entity.ChatMessage;
import lombok.*;

import java.time.LocalDateTime;
//record - 자바에서 불변(immutable) 데이터 전달용 클래스를 간단히 정의하는 방법.
public record ChatMessageDto(
        String messageId,       // 메시지 PK (커서용 + 정렬)
        String roomId,        // 방 ID
        String accountEmail,  // 보낸 사람 식별자
        String message,       // 메시지 본문
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt // 생성 시각
) {
    public static ChatMessageDto from(ChatMessage m) {
        return new ChatMessageDto(
                m.getMessageId(),
                m.getRoomId(),
                m.getAccountEmail(),
                m.getMessage(),
                m.getCreatedAt()
        );
    }
}