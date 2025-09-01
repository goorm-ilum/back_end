package com.talktrip.talktrip.domain.chat.controller;

import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
import com.talktrip.talktrip.domain.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatWebSocketControllerTest {

    @InjectMocks
    private ChatWebSocketController chatWebSocketController;

    @Mock
    private ChatService chatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private Principal mockPrincipal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 테스트 사용자 Principal 설정
        mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("testuser@test.com");
    }

    @Test
    @DisplayName("정상적인 메시지 처리")
    void handleMessage_success() {
        // Given
        ChatMessageRequestDto dto = new ChatMessageRequestDto("ROOM_001", "testuser@test.com", "Hello, World!");

        // When
        chatWebSocketController.handleMessage(dto, mockPrincipal);

        // Then
        verify(chatService, times(1)).saveAndSend(eq(dto), eq(mockPrincipal)); // 서비스 호출 검증
    }

    @Test
    @DisplayName("메시지 처리 중 서비스 예외 발생")
    void handleMessage_serviceException() {
        // Given
        ChatMessageRequestDto dto = new ChatMessageRequestDto("ROOM_001", "testuser@test.com", "Hello, World!");
        doThrow(new RuntimeException("서비스 오류 발생"))
                .when(chatService).saveAndSend(eq(dto), eq(mockPrincipal));

        // When
        assertThatThrownBy(() -> chatWebSocketController.handleMessage(dto, mockPrincipal))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("서비스 오류 발생");

        // Then
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("메시지 처리 중 Null DTO 발생")
    void handleMessage_nullDto() {
        // Given
        ChatMessageRequestDto dto = null;

        // When & Then
        assertThatThrownBy(() -> chatWebSocketController.handleMessage(dto, mockPrincipal))
                .isInstanceOf(NullPointerException.class); // NPE 발생 예상
        verify(chatService, never()).saveAndSend(any(), any());
    }

    @Test
    @DisplayName("Principal 이름이 null인 경우 (인증 정보 없음)")
    void handleMessage_nullPrincipalName() {
        // Given
        when(mockPrincipal.getName()).thenReturn(null); // 인증 정보 없음
        ChatMessageRequestDto dto = new ChatMessageRequestDto("ROOM_001", null, "Hello, World!");

        // When & Then
        assertThatThrownBy(() -> chatWebSocketController.handleMessage(dto, mockPrincipal))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Principal name is required");

        verify(chatService, never()).saveAndSend(any(), any());
    }

    @Test
    @DisplayName("서비스 호출 없이 사용자에게 에러 메시지 전송 (Redis 관련 예외 처리)")
    void handleMessage_sendErrorToUser() {
        // Given
        ChatMessageRequestDto dto = new ChatMessageRequestDto("ROOM_001", "testuser@test.com", "Hello, World!");
        doThrow(new RuntimeException("Redis 예외 발생"))
                .when(chatService).saveAndSend(eq(dto), eq(mockPrincipal));

        // When
        assertThatThrownBy(() -> chatWebSocketController.handleMessage(dto, mockPrincipal))
                .isInstanceOf(RuntimeException.class);

        // Then
        verify(messagingTemplate, times(1))
                .convertAndSendToUser(eq("testuser@test.com"), eq("/queue/errors"), contains("예외 발생"));
    }

    @Test
    @DisplayName("DTO가 비어 있을 경우")
    void handleMessage_emptyDto() {
        // Given
        ChatMessageRequestDto dto = new ChatMessageRequestDto("", "testuser@test.com", "");

        // When & Then
        assertThatThrownBy(() -> chatWebSocketController.handleMessage(dto, mockPrincipal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("메시지 요청 DTO가 비어 있음");

        verify(chatService, never()).saveAndSend(any(), any());
    }
}