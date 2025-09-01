package com.talktrip.talktrip.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.connection.MessageListener;

@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {

    //private final ObjectMapper objectMapper; // 공용 ObjectMapper를 주입받음

    @Bean
    public ChannelTopic topic() {
        return new ChannelTopic("chat.message");
    }

    @Bean
    public ChannelTopic roomUpdateTopic() {
        return new ChannelTopic("chat.room.update");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Qualifier("redisSubscriber") MessageListener subscriber // MessageListener 주입
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 패턴 기반 메시지 리스너 추가
        container.addMessageListener(subscriber, new PatternTopic("chat:room:*"));
        container.addMessageListener(subscriber, new PatternTopic("chat:user:*"));

        return container;
    }


}
