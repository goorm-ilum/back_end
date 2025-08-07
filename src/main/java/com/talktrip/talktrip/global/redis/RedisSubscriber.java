package com.talktrip.talktrip.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
import com.talktrip.talktrip.domain.chat.message.dto.ChatRoomUpdateMessage;
import com.talktrip.talktrip.domain.chat.message.dto.ChatUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            log.debug("🔥 Redis에서 받은 payload: {}", payload);

            // 페이로드가 이미 JSON 문자열 형태로 이중 직렬화되어 있는 경우
            if (payload.startsWith("\"") && payload.endsWith("\"")) {
                // 이중 직렬화된 문자열을 먼저 일반 JSON 문자열로 변환
                payload = objectMapper.readValue(payload, String.class);
            }
        
            switch (channel) {
                case "chat.message":
                    handleMessage(payload, ChatRoomUpdateMessage.class, "/topic/chat/room/");
                    break;
                
                case "chat.room.update":
                    handleMessage(payload, ChatUpdateMessage.class, "/topic/chat/room/", "/update");
                    break;
                
                default:
                    log.warn("처리되지 않은 채널: {}", channel);
                    break;
            }
        
        } catch (Exception e) {
            log.error("메시지 처리 중 에러 발생: ", e);
        }
    }

    private <T> void handleMessage(String payload, Class<T> valueType, String destination, String... additionalPath) {
        try {
            T message = objectMapper.readValue(payload, valueType);
            String roomId = getRoomId(message);
            String fullDestination = destination + roomId + String.join("", additionalPath);
            messagingTemplate.convertAndSend(fullDestination, message);
        } catch (Exception e) {
            log.error("메시지 변환 중 에러 발생: ", e);
        }
    }

    private String getRoomId(Object message) {
        if (message instanceof ChatMessageRequestDto) {
            return ((ChatMessageRequestDto) message).getRoomId();
        } else if (message instanceof ChatUpdateMessage) {
            return ((ChatUpdateMessage) message).getRoomId();
        } else if (message instanceof ChatRoomUpdateMessage ) {
            return ((ChatRoomUpdateMessage) message).getRoomId();

        }
        throw new IllegalArgumentException("지원하지 않는 메시지 타입입니다");
    }
}