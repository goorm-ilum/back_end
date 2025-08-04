package com.talktrip.talktrip.domain.chat.message.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MessageRequestDto {
    private String content;
    private String createdAt;
    private String roomId;
    private String userId;
}
