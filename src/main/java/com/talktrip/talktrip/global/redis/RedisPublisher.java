package com.talktrip.talktrip.global.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.chat.dto.response.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Log4j2

public class RedisPublisher {
    private final StringRedisTemplate redis;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisPublisherWithRetry publisherWithRetry; // ← 여기서 사용

    public void publish(String channel, Object payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            redis.convertAndSend(channel, message);
            log.info("[RedisPublisher] Redis로 메시지 발행 (Channel: {}, Message: {})", channel, message);
        } catch (Exception e) {
            log.error("[RedisPublisher] Redis 메시지 발행 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    public void publishAfterCommit(ChannelTopic topic, Object dto) {
        runAfterCommit(() -> doPublish(topic, dto)); // 커밋 후 발행 (재시도 X)
    }

    public void publishWithRetry(ChannelTopic topic, Object dto) {
        // 재시도 필요하지만 트랜잭션 밖: 즉시 재시도 발행
        publisherWithRetry.publishWithRetry(topic, dto);
    }

    public void publishAfterCommitWithRetry(ChannelTopic topic, Object dto) {
        // 커밋 보장 + 일시 오류 재시도
        runAfterCommit(() -> publisherWithRetry.publishWithRetry(topic, dto));
    }

    public void publishToRoom(String roomId, ChatMessageDto dto) {
        try {
            String channel = "chat:room:" + roomId; // ✅ 방별 채널
            redis.convertAndSend(channel, objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new RuntimeException("Redis publish failed", e);
        }
    }


    private void doPublish(ChannelTopic topic, Object dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            log.info("🚀 Redis에 발행할 JSON: {}", json);
            redisTemplate.convertAndSend(topic.getTopic(), json);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("메시지 직렬화 실패", e);
        }
    }

    private void runAfterCommit(Runnable task) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCommit() { task.run(); }
                    }
            );
        } else {
            task.run();
        }
    }
}
