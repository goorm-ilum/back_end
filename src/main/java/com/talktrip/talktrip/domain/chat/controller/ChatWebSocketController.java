package com.talktrip.talktrip.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
import com.talktrip.talktrip.domain.chat.message.dto.ChatUpdateMessage;
import com.talktrip.talktrip.domain.chat.service.ChatService;
import com.talktrip.talktrip.global.redis.RedisPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
@Slf4j
@RequiredArgsConstructor
@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final RedisPublisher redisPublisher;
    private final ChannelTopic topic;
    private final ChannelTopic roomUpdateTopic;  // Redis 채널 토픽 추가
    private final ObjectMapper objectMapper;

    @MessageMapping("/chat/message")  // 클라이언트 → /app/chat/message
    public void handleMessage(ChatMessageRequestDto dto) {
        chatService.saveAndSend(dto);
    }
    
    // 클라이언트가 구독할 엔드포인트:
    // 채팅 메시지: /topic/chat/room/{roomId}
    // 채팅방 업데이트: /topic/chat/room/{roomId}/update
}