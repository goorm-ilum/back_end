package com.talktrip.talktrip.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
import com.talktrip.talktrip.domain.chat.dto.response.ChatMessageDto;
import com.talktrip.talktrip.domain.chat.dto.response.ChatMessagePush;
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
@Component("redisSubscriber") // ✅ 이 이름으로 MessageListener 빈이 딱 1개 존재하게 유지
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final String instanceId; // websocket 인스턴스 식별자 주입


    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("[{}][RedisSubscriber] received channel={}, bytes={}", instanceId, channel, payload.length());


            if (payload.startsWith("\"") && payload.endsWith("\"")) {
                // 이중 직렬화된 문자열을 먼저 일반 JSON 문자열로 변환
                payload = objectMapper.readValue(payload, String.class);
            }

            if (channel.startsWith("chat:room:")) {
                // 방 브로드캐스트
                ChatMessagePush dto = objectMapper.readValue(payload, ChatMessagePush.class);
                String dest = "/topic/chat/room/" + dto.getRoomId();    // 프론트 구독 경로
                messagingTemplate.convertAndSend(dest, dto);
                log.info("[{}][RedisSubscriber] forwarded -> dest={}, msgId={}", instanceId, dest, dto.getMessageId());



            } else if (channel.startsWith("chat:user:")) {
                // 개인 사이드바 업데이트 (convertAndSendToUser)
                ChatRoomUpdateMessage dto = objectMapper.readValue(payload, ChatRoomUpdateMessage.class);
                String userEmail = channel.substring("chat:user:".length());
                messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat/rooms", dto);
                log.info("[{}][RedisSubscriber] forwarded -> user={}, dest=/queue/chat/rooms", instanceId, userEmail);


            } else {
                log.warn("처리되지 않은 채널: {}", channel);
            }
        } catch (Exception e) {
            log.error("Redis payload 처리 실패", e);
        }
    }

    private <T> void handleMessage(String payload, Class<T> type, String prefix, String... suffix) {
        try {
            T dto = objectMapper.readValue(payload, type);

            String roomId = null;
            if (dto instanceof ChatRoomUpdateMessage m) roomId = m.getRoomId();
            if (roomId == null || roomId.isBlank()) {
                log.warn("roomId가 비어있어 메시지를 전송하지 않습니다. payload={}", payload);
                return;
            }

            String dest = prefix + roomId + (suffix != null ? String.join("", suffix) : "");
            messagingTemplate.convertAndSend(dest, dto);
        } catch (Exception e) {
            log.error("Redis payload 처리 실패: {}", e.getMessage(), e);
        }
    }

    private <T> void handleMessage(String payload, Class<T> type, String prefix) {
        handleMessage(payload, type, prefix, "");
    }

    private String getRoomId(Object message) {
        if (message instanceof ChatMessageRequestDto m) return m.getRoomId();
        if (message instanceof ChatUpdateMessage m) return m.getRoomId();
        if (message instanceof ChatRoomUpdateMessage m) return m.getRoomId();
        throw new IllegalArgumentException("지원하지 않는 메시지 타입입니다");
    }
}

