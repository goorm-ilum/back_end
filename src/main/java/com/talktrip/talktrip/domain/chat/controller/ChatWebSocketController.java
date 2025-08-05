package com.talktrip.talktrip.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
import com.talktrip.talktrip.domain.chat.service.ChatService;
import com.talktrip.talktrip.global.redis.RedisPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final RedisPublisher redisPublisher;
    private final ChannelTopic topic;
    private final ObjectMapper objectMapper;


    @MessageMapping("/chat/message") // 클라이언트 → /app/chat/message
    public void handleMessage(ChatMessageRequestDto dto)
    {
        chatService.saveAndSend(dto);
    }
}

