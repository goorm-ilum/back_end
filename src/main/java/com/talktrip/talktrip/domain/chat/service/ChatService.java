package com.talktrip.talktrip.domain.chat.service;

import com.talktrip.talktrip.domain.chat.dto.request.ChatMessage;
import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageDto;
import com.talktrip.talktrip.domain.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate; // WebSocket 용
    private final ChatMessageRepository chatMessageRepository;

    public void saveAndSend(ChatMessageDto dto) {
        ChatMessage saved = chatMessageRepository.save(
                new ChatMessage(dto.getRoomId(), dto.getSenderId(), dto.getMessage(), LocalDateTime.now())
        );

        messagingTemplate.convertAndSend("/topic/chat/" + dto.getRoomId(), dto); // 실시간 전송
    }

    public List<ChatMessageDto> getMessages(String roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAt(roomId)
                .stream().map(ChatMessageDto::fromEntity).toList();
    }

    public String createRoom(String userA, String userB) {
        // 기존 방 있으면 재사용, 없으면 새로 생성
        // room_id = UUID.randomUUID().toString()
        return userA;
    }
}