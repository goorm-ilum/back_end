package com.talktrip.talktrip.domain.chat.message.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MessageRequestDto {
    private String content;
    private String createdAt;
    private String roomId;
    private String accountEmail;
}
