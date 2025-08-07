package com.talktrip.talktrip.domain.chat.message.dto;

import com.talktrip.talktrip.domain.chat.entity.ChatMessage;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.CurrentTimestamp;

import java.sql.Timestamp;

@Builder
@Getter
public class ChatRoomUpdateMessage {
    private String memberId;
    private String roomId;        // 어떤 방이 변경됐는지
    private String message;   // 마지막 메시지
    private int notReadMessageCount;      // 안 읽은 메시지 수
    private String receiverId;  // 수신자 ID (알림 받을 사람)
    private Timestamp updatedAt;

    public ChatRoomUpdateMessage toEntity() {
        return ChatRoomUpdateMessage.builder()
                .memberId(memberId)
                .roomId(roomId)
                .message(message)
                .notReadMessageCount(notReadMessageCount)
                .receiverId(receiverId)
                .updatedAt(updatedAt)
                .build();
    }
}
