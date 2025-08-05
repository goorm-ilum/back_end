package com.talktrip.talktrip.domain.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.talktrip.talktrip.domain.chat.entity.ChatMessage;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ChatMessageRequestDto {
    private String roomId;
    private String memberId;
    private String message;

    @JsonCreator
    public ChatMessageRequestDto(
            @JsonProperty("roomId") String roomId,
            @JsonProperty("memberId") String memberId,
            @JsonProperty("message") String message) {
        this.roomId = roomId;
        this.memberId = memberId;
        this.message = message;
    }

    public ChatMessage toEntity() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String messageId = "mgs" + uuid.substring(0, 7);

        return new ChatMessage(
                messageId,
                this.roomId,
                this.memberId,
                this.message,
                null
        );
    }
}