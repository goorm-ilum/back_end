package com.talktrip.talktrip.domain.chat.dto.request;

import lombok.Data;

@Data
public class CreateRoomRequest {
    private String userA;
    private String userB;
}