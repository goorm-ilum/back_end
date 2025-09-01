package com.talktrip.talktrip.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
public class ChatMessagePush {
    private String messageId;
    private String roomId;
    private String sender;       // accountEmail
    private String senderName;   // 발신자 이름
    private String message;
    private String createdAt;    // ISO8601 string

    @JsonCreator //redis -  Jackson에게 역직렬화에 사용할 생성자 지정
    public ChatMessagePush(
            @JsonProperty("messageId") String messageId,
            @JsonProperty("roomId") String roomId,
            @JsonProperty("sender") String sender,
            @JsonProperty("senderName") String senderName,
            @JsonProperty("message") String message,
            @JsonProperty("createdAt") String createdAt
    ) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.sender = sender;
        this.senderName = senderName;
        this.message = message;
        this.createdAt = createdAt;
    }
}