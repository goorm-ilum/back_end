package com.talktrip.talktrip.domain.chat.controller;

import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageDto;
import com.talktrip.talktrip.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/message") // 클라이언트 → /app/chat/message
    public void handleMessage(ChatMessageDto dto) {
        chatService.saveAndSend(dto);
    }
}

