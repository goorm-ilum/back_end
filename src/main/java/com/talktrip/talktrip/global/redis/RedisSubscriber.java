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
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        
            switch (channel) {
                case "chat.message":
                    handleMessage(payload, ChatRoomUpdateMessage.class, "/topic/chat/room/");
                    break;
                
                case "chat.room.update":
                    handleMessage(payload, ChatRoomUpdateMessage.class, "/topic/chat/room/", "/update");
                    break;
                
                default:
                    log.warn("처리되지 않은 채널: {}", channel);
                    break;
            }
        
        } catch (Exception e) {
            log.error("메시지 처리 중 에러 발생: ", e);
        }
    }
    // prefix + suffix (가변 인자 버전)
    private <T> void handleMessage(String payload, Class<T> type, String prefix, String... suffix) {
        try {
            // ✅ 이중 직렬화 언래핑 없이 한 번만 파싱
            T dto = objectMapper.readValue(payload, type);

            // ✅ roomId 추출 (필요시 타입 분기)
            String roomId = null;
            if (dto instanceof ChatRoomUpdateMessage m) {
                roomId = m.getRoomId();
            }
            if (roomId == null || roomId.isBlank()) {
                log.warn("roomId가 비어있어 메시지를 전송하지 않습니다. payload={}", payload);
                return;
            }

            String dest = prefix + roomId + (suffix != null ? String.join("", suffix) : "");
            messagingTemplate.convertAndSend(dest, dto); // STOMP로 그대로 전달
        } catch (Exception e) {
            log.error("Redis payload 처리 실패: {}", e.getMessage(), e);
        }
    }
    // prefix만 있는 경우
    private <T> void handleMessage(String payload, Class<T> type, String prefix) {
        handleMessage(payload, type, prefix, "");
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