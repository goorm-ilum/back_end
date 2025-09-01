package com.talktrip.talktrip.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * 테스트 환경에서 WebSocket 설정을 완화하는 설정
 * 실제 운영 환경에서는 사용하지 않습니다.
 */
@TestConfiguration
@Profile("test")
@EnableWebSocketMessageBroker
public class TestWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 테스트 환경에서는 모든 Origin 허용
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // 모든 Origin 허용
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");
        
        // 네이티브 WebSocket 엔드포인트 추가
        registry.addEndpoint("/ws/websocket")
                .setAllowedOriginPatterns("*")
                .setAllowedOrigins("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 테스트 환경에서는 메시지 브로커 설정만 하고 인터셉터는 추가하지 않음
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        // 테스트 환경에서는 기본 메시지 컨버터만 사용
        return false;
    }
}
