package com.talktrip.talktrip.domain.chat.message.dto;

import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;

@Builder
@Getter
public class ChatRoomUpdateMessage {
    private String accountEmail;             // 보낸 사람 이메일(발신자)
    private String roomId;                   // 방 ID
    private String message;                  // 마지막 메시지
    private int notReadMessageCount;         // 하위 호환: 수신자 기준 값으로 채움
    private String receiverAccountEmail;     // 수신자 이메일
    private Timestamp updatedAt;

    // 신규 필드: 사용자별 읽지 않음 수
    private int unreadCountForSender;        // 발신자 기준
    private int unreadCountForReceiver;      // 수신자 기준

    public ChatRoomUpdateMessage toEntity() {
        return ChatRoomUpdateMessage.builder()
                .accountEmail(accountEmail)
                .roomId(roomId)
                .message(message)
                .notReadMessageCount(notReadMessageCount)
                .receiverAccountEmail(receiverAccountEmail)
                .updatedAt(updatedAt)
                .unreadCountForSender(unreadCountForSender)
                .unreadCountForReceiver(unreadCountForReceiver)
                .build();
    }
}
