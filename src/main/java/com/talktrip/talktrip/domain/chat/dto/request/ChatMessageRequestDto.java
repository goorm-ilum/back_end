package com.talktrip.talktrip.domain.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.talktrip.talktrip.domain.chat.entity.ChatMessage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CurrentTimestamp;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ChatMessageRequestDto {
    private String roomId;
    private String accountEmail;
    private String message;
    private String receiverAccountEmail;

    @JsonCreator
    public ChatMessageRequestDto(
            @JsonProperty("roomId") String roomId,
            @JsonProperty("accountEmail") String accountEmail,
            @JsonProperty("message") String message,
            @JsonProperty("receiverAccountEmail") String receiverAccountEmail

            ) {
        this.roomId = roomId;
        this.accountEmail = accountEmail;
        this.message = message;
        this.receiverAccountEmail=receiverAccountEmail;

    }

    public ChatMessage toEntity() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String messageId = "mgs" + uuid.substring(0, 7);

        return new ChatMessage(
                messageId,
                this.roomId,
                this.accountEmail,
                this.message,
                null
        );
    }
}