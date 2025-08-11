package com.talktrip.talktrip.domain.chat.message.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CurrentTimestamp;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatUpdateMessage {
    private String accountEmail;
    private String roomId;
    private String messageType;    // "READ_UPDATE" 같은 메시지 타입을 지정할 수 있음
    @CurrentTimestamp
    private Long timestamp;

    // 간단한 생성을 위한 생성자
    public ChatUpdateMessage(String accountEmail) {
        this.accountEmail = accountEmail;
        this.messageType = "READ_UPDATE";
        this.timestamp = System.currentTimeMillis();
    }

}