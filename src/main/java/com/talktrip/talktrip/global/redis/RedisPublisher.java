package com.talktrip.talktrip.global.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Log4j2
public class RedisPublisher {
    private final RedisPublisherWithRetry publisherWithRetry;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper; // ✅ 주입 받기


    public void publish(ChannelTopic topic, Object dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            log.info("🚀 Redis에 발행할 JSON: {}", json);

            redisTemplate.convertAndSend(topic.getTopic(), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("메시지 직렬화 실패", e);
        }
    }


}