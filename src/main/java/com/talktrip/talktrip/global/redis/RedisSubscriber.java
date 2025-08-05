package com.talktrip.talktrip.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
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
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            log.debug("🔥 Redis에서 받은 payload: {}", payload);
            
            // 페이로드가 이미 JSON 문자열 형태로 이중 직렬화되어 있는 경우
            if (payload.startsWith("\"") && payload.endsWith("\"")) {
                // 이중 직렬화된 문자열을 먼저 일반 JSON 문자열로 변환
                payload = objectMapper.readValue(payload, String.class);
            }
            
            // JSON 문자열을 ChatMessageRequestDto 객체로 변환
            ChatMessageRequestDto chatMessage = objectMapper.readValue(payload, ChatMessageRequestDto.class);
            log.info("✅ 변환된 메시지: {}", chatMessage);

            // WebSocket으로 전송
            messagingTemplate.convertAndSend("/topic/chat/" + chatMessage.getRoomId(), chatMessage);
        } catch (Exception e) {
            log.error("메시지 처리 실패: {}", e.getMessage());
            log.error("상세 에러: ", e);
        }
    }
}