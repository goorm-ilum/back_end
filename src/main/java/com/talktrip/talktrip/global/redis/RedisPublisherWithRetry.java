package com.talktrip.talktrip.global.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
import com.talktrip.talktrip.global.config.RedisRetryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisPublisherWithRetry {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisRetryProperties retryProperties;

    public void publishWithRetry(ChannelTopic topic, ChatMessageRequestDto message) {
        int attempts = 0;
        long backoff = retryProperties.getBackoffInitial();

        while (attempts < retryProperties.getMaxAttempts()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                redisTemplate.convertAndSend(topic.getTopic(), jsonMessage);
                log.info("메시지 발행 성공 - 시도 횟수: {}", attempts + 1);
                return;
            } catch (RedisConnectionFailureException | JsonProcessingException e) {
                attempts++;

                if (attempts >= retryProperties.getMaxAttempts()) {
                    log.error("최대 재시도 횟수 초과. 메시지 발행 실패: {}", message, e);
                    throw new RuntimeException("Redis 메시지 발행 실패", e);
                }

                log.warn("메시지 발행 실패, {}ms 후 재시도 ({}/{})",
                        backoff, attempts, retryProperties.getMaxAttempts());

                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                }

                // 다음 재시도를 위한 backoff 시간 계산
                backoff = Math.min(
                        (long) (backoff * retryProperties.getBackoffMultiplier()),
                        retryProperties.getBackoffMax()
                );
            }
        }
    }
}