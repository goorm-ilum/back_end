package com.talktrip.talktrip.domain.chat.message.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Builder
@Getter
@NoArgsConstructor(force = true) // 강제로 기본 생성자 활성화
public class ChatRoomUpdateMessage {
    private final String accountEmail;             // 발신자 이메일
    private final String roomId;                  // 방 ID
    private final String message;                 // 마지막 메시지
    private final String messageId;               // 메시지 ID
    private final String senderAccountEmail;      // 발신자 이름
    private final LocalDateTime createdAt;        // 메시지 생성 시간
    private final int notReadMessageCount;        // 수신자의 읽지 않은 메시지 개수
    private final String receiverAccountEmail;    // 수신자 이메일
    private final Timestamp updatedAt;           // 최종 업데이트 시간
    private final int unreadCountForSender;       // 발신자 기준 읽지 않음 수
    private final int unreadCountForReceiver;     // 수신자 기준 읽지 않음 수

    @JsonCreator //redis -  Jackson에게 역직렬화에 사용할 생성자 지정
    public ChatRoomUpdateMessage(
            @JsonProperty("accountEmail") String accountEmail,
            @JsonProperty("roomId") String roomId,
            @JsonProperty("message") String message,
            @JsonProperty("messageId") String messageId,
            @JsonProperty("senderAccountEmail") String senderAccountEmail,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("notReadMessageCount") int notReadMessageCount,
            @JsonProperty("receiverAccountEmail") String receiverAccountEmail,
            @JsonProperty("updatedAt") Timestamp updatedAt,
            @JsonProperty("unreadCountForSender") int unreadCountForSender,
            @JsonProperty("unreadCountForReceiver") int unreadCountForReceiver
    ) {
        this.accountEmail = accountEmail;
        this.roomId = roomId;
        this.message = message;
        this.messageId = messageId;
        this.senderAccountEmail = senderAccountEmail;
        this.createdAt = createdAt;
        this.notReadMessageCount = notReadMessageCount;
        this.receiverAccountEmail = receiverAccountEmail;
        this.updatedAt = updatedAt;
        this.unreadCountForSender = unreadCountForSender;
        this.unreadCountForReceiver = unreadCountForReceiver;
    }

    public ChatRoomUpdateMessage toEntity() {
        return ChatRoomUpdateMessage.builder()
                .accountEmail(accountEmail)
                .roomId(roomId)
                .message(message)
                .messageId(messageId)
                .senderAccountEmail(senderAccountEmail)
                .createdAt(createdAt)
                .notReadMessageCount(notReadMessageCount)
                .receiverAccountEmail(receiverAccountEmail)
                .updatedAt(updatedAt)
                .unreadCountForSender(unreadCountForSender)
                .unreadCountForReceiver(unreadCountForReceiver)
                .build();
    }
}
