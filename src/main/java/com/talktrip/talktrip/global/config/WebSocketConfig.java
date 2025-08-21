package com.talktrip.talktrip.global.config;

import com.talktrip.talktrip.global.interceptor.JwtStompChannelInterceptor;
import com.talktrip.talktrip.global.interceptor.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor); // 👈 인터셉터 등록
    }
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")// localhost:8080/ws
                .setAllowedOrigins("http://localhost:5173") // ✅ 프론트 주소들 추가
                .addInterceptors(handshakeInterceptor) // ✅ 꼭 넣기
                .withSockJS()
                .setWebSocketEnabled(true)
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000)
                .setHttpMessageCacheSize(1000)
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(1000)
                .setSessionCookieNeeded(false);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 브로커 목적지: topic(그룹), queue(포인트투포인트 용어) 둘 다 활성화
        registry.enableSimpleBroker("/topic", "/queue");// 구독 url,서버 → 클라이언트// 브로커(바로 브로드캐스트)
        registry.setApplicationDestinationPrefixes("/app");// prefix 정의, 클라이언트 → 서버 // 컨트롤러로 가는 입구

        // kafka, rabbitmq 여기서 설정
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(128 * 1024);
        registration.setSendBufferSizeLimit(512 * 1024);
        registration.setSendTimeLimit(20000);
        registration.setTimeToFirstMessage(30000);
    }

}
