package com.talktrip.talktrip.domain.chat.controller;

import com.talktrip.talktrip.domain.chat.dto.response.ChatMessagePush;
import com.talktrip.talktrip.global.redis.RedisMessageBroker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(MockitoExtension.class)
class TestRedisControllerTest {

    @Mock
    private RedisMessageBroker redisMessageBroker;

    @InjectMocks
    private TestRedisController testRedisController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(testRedisController).build();
    }

    @Test
    @DisplayName("Redis 테스트 메시지 발행 - 성공")
    void publishTestMessage_success() throws Exception {
        // Given
        String channel = "test-channel";
        String roomId = "ROOM_001";
        String message = "테스트 메시지입니다.";

        doNothing().when(redisMessageBroker).publish(eq(channel), any(ChatMessagePush.class));

        // When
        var result = mockMvc.perform(post("/api/test/publish")
                        .param("channel", channel)
                        .param("roomId", roomId)
                        .param("message", message))
                .andReturn();

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains("Test");
        assertThat(result.getResponse().getContentAsString()).contains("Redis");

        verify(redisMessageBroker, times(1)).publish(eq(channel), any(ChatMessagePush.class));
    }

    @Test
    @DisplayName("Redis 테스트 메시지 발행 - 빈 메시지")
    void publishTestMessage_emptyMessage() throws Exception {
        // Given
        String channel = "test-channel";
        String roomId = "ROOM_001";
        String message = "";

        doNothing().when(redisMessageBroker).publish(eq(channel), any(ChatMessagePush.class));

        // When
        var result = mockMvc.perform(post("/api/test/publish")
                        .param("channel", channel)
                        .param("roomId", roomId)
                        .param("message", message))
                .andReturn();

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains("Test");
        assertThat(result.getResponse().getContentAsString()).contains("Redis");

        verify(redisMessageBroker, times(1)).publish(eq(channel), any(ChatMessagePush.class));
    }

    @Test
    @DisplayName("Redis 테스트 메시지 발행 - 특수문자 포함 메시지")
    void publishTestMessage_specialCharacters() throws Exception {
        // Given
        String channel = "test-channel";
        String roomId = "ROOM_001";
        String message = "안녕하세요! @#$%^&*() 테스트 메시지입니다.";

        doNothing().when(redisMessageBroker).publish(eq(channel), any(ChatMessagePush.class));

        // When
        var result = mockMvc.perform(post("/api/test/publish")
                        .param("channel", channel)
                        .param("roomId", roomId)
                        .param("message", message))
                .andReturn();

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains("Test");
        assertThat(result.getResponse().getContentAsString()).contains("Redis");

        verify(redisMessageBroker, times(1)).publish(eq(channel), any(ChatMessagePush.class));
    }

    @Test
    @DisplayName("Redis 테스트 메시지 발행 - 긴 메시지")
    void publishTestMessage_longMessage() throws Exception {
        // Given
        String channel = "test-channel";
        String roomId = "ROOM_001";
        String message = "이것은 매우 긴 테스트 메시지입니다. ".repeat(10);

        doNothing().when(redisMessageBroker).publish(eq(channel), any(ChatMessagePush.class));

        // When
        var result = mockMvc.perform(post("/api/test/publish")
                        .param("channel", channel)
                        .param("roomId", roomId)
                        .param("message", message))
                .andReturn();

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains("Test");
        assertThat(result.getResponse().getContentAsString()).contains("Redis");

        verify(redisMessageBroker, times(1)).publish(eq(channel), any(ChatMessagePush.class));
    }

    @Test
    @DisplayName("Redis 테스트 메시지 발행 - 파라미터 검증")
    void publishTestMessage_parameterValidation() throws Exception {
        // Given
        String channel = "test-channel";
        String roomId = "ROOM_001";
        String message = "테스트 메시지";

        doNothing().when(redisMessageBroker).publish(eq(channel), any(ChatMessagePush.class));

        // When
        var result = mockMvc.perform(post("/api/test/publish")
                        .param("channel", channel)
                        .param("roomId", roomId)
                        .param("message", message))
                .andReturn();

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        
        // ChatMessagePush 객체가 올바른 파라미터로 생성되었는지 검증
        verify(redisMessageBroker, times(1)).publish(
                eq(channel), 
                argThat(push -> {
                    assertThat(push).isInstanceOf(ChatMessagePush.class);
                    ChatMessagePush chatPush = (ChatMessagePush) push;
                    assertThat(chatPush.getMessageId()).isEqualTo("TEST_MSG_ID");
                    assertThat(chatPush.getRoomId()).isEqualTo(roomId);
                    assertThat(chatPush.getSender()).isEqualTo("testuser@test.com");
                    assertThat(chatPush.getSenderName()).isEqualTo("Tester");
                    assertThat(chatPush.getMessage()).isEqualTo(message);
                    assertThat(chatPush.getCreatedAt()).isNotNull();
                    return true;
                })
        );
    }

    @Test
    @DisplayName("Redis 테스트 메시지 발행 - 응답 내용 검증")
    void publishTestMessage_responseContentValidation() throws Exception {
        // Given
        String channel = "test-channel";
        String roomId = "ROOM_001";
        String message = "응답 검증 테스트";

        doNothing().when(redisMessageBroker).publish(eq(channel), any(ChatMessagePush.class));

        // When
        var result = mockMvc.perform(post("/api/test/publish")
                        .param("channel", channel)
                        .param("roomId", roomId)
                        .param("message", message))
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(responseContent).contains("[TestRedisController]");
        assertThat(responseContent).contains("Test");
        assertThat(responseContent).contains("Redis");
        assertThat(responseContent).doesNotContain("error");
        assertThat(responseContent).doesNotContain("실패");

        verify(redisMessageBroker, times(1)).publish(eq(channel), any(ChatMessagePush.class));
    }
}
