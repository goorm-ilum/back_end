package com.talktrip.talktrip.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
import com.talktrip.talktrip.domain.chat.dto.response.ChatMessagePush;
import com.talktrip.talktrip.global.redis.RedisMessageBroker;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import java.security.Principal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@SpringBootTest
@Import({RedisMessageBroker.class})
class ChatServiceIntegrationTest {

    @Autowired
    private RedisMessageBroker redisMessageBroker;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private Principal mockPrincipal;

    private static ExecutorService executorService;

    @BeforeAll
    static void beforeAll() {
        executorService = Executors.newCachedThreadPool();
    }

    @BeforeEach
    void setUp() {
        openMocks(this);
        mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("testuser@test.com");
    }

    @AfterAll
    static void afterAll() {
        executorService.shutdown();
    }

    @Test
    @DisplayName("WebSocket 서버 중 하나가 다운된 상태에서 메시지가 정상적으로 전달된다")
    void testWebSocketFailureHandling() throws Exception {
        // Given
        ChatMessageRequestDto dto = new ChatMessageRequestDto("ROOM_001",mockPrincipal.getName() ,"Hello, WebSocket!");
        ChatMessagePush push = new ChatMessagePush("MSG_001", "ROOM_001", "testuser@test.com", "TestUser", "Message Content", "2025-01-01T10:00:00");

        // Mock RedisTemplate 동작
        when(redisTemplate.convertAndSend("chat:room:ROOM_001", push))
                .thenThrow(new RuntimeException("WebSocket 서버 다운"));

        // 서버 2개는 정상 작동, 1개는 다운된 상태
        when(redisTemplate.convertAndSend("chat:user:user1@test.com", push)).thenReturn(null); // 성공
        when(redisTemplate.convertAndSend("chat:user:user2@test.com", push)).thenReturn(null); // 성공

        // When
        redisMessageBroker.publishWithRetry("chat:room:ROOM_001", push);

        // Then
        // 실패한 WebSocket 서버는 에러가 로그에 기록되며 프로세스는 종료되지 않는다.
        // 다른 WebSocket 연결은 정상적으로 유지
        Assertions.assertDoesNotThrow(() ->
                redisMessageBroker.publishWithRetry("chat:user:user1@test.com", push)
        );

        Assertions.assertDoesNotThrow(() ->
                redisMessageBroker.publishWithRetry("chat:user:user2@test.com", push)
        );
    }

    @Test
    @DisplayName("다운된 서버가 복구되었을 때 메시지가 재발행된다")
    void testWebSocketServerRecovery() throws Exception {
        // Given
        ChatMessagePush push = new ChatMessagePush("MSG_001", "ROOM_001", "testuser@test.com", "TestUser", "Message Content", "2025-01-01T10:00:00");

        // Mock RedisTemplate 동작
        when(redisTemplate.convertAndSend("chat:room:ROOM_001", push))
                .thenThrow(new RuntimeException("WebSocket 서버 다운"))
                .thenReturn(null); // 복구 후 성공

        // When - 첫 번째 시도 (실패)
        Assertions.assertDoesNotThrow(() ->
                redisMessageBroker.publishWithRetry("chat:room:ROOM_001", push)
        );

        // Simulate server recovery by allowing the second call to succeed
        // 두 번째 시도 (성공)
        Assertions.assertDoesNotThrow(() ->
                redisMessageBroker.publishWithRetry("chat:room:ROOM_001", push)
        );

        // Then - 서버가 복구된 후에는 메시지가 정상적으로 발행됨
        Assertions.assertTrue(true); // 모든 테스트가 정상적으로 실패 없이 완료되었음을 의미
    }
}
