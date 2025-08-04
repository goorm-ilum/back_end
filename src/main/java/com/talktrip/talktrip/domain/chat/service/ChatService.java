package com.talktrip.talktrip.domain.chat.service;

import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageDto;
import com.talktrip.talktrip.domain.chat.entity.ChatMessageHistory;
import com.talktrip.talktrip.domain.chat.entity.ChatRoom;
import com.talktrip.talktrip.domain.chat.repository.ChatMessageHistoryRepository;
import com.talktrip.talktrip.domain.chat.repository.ChatMessageRepository;
import com.talktrip.talktrip.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate; // WebSocket 용
    private final ChatMessageHistoryRepository chatMessageHistoryRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public void saveAndSend(ChatMessageDto dto) {
        ChatMessageHistory saved = chatMessageHistoryRepository.save(
                new ChatMessageHistory(dto.getMessageId(),dto.getRoomId(), dto.getSenderId(), dto.getMessage(), LocalDateTime.now())
        );

        messagingTemplate.convertAndSend("/topic/chat/" + dto.getRoomId(), dto); // 실시간 전송
    }
    public String createRoom(String userA, String userB) {
        // 기존 방 있으면 재사용, 없으면 새로 생성
        // room_id = UUID.randomUUID().toString()
        return userA;
    }
    public List<ChatRoom> getRooms(String memberId) {
        return chatRoomRepository.findRoomsWithLastMessageByMemberId(memberId);
    }
    public int getCountALLUnreadMessagesRooms(String userId) {
        return chatMessageRepository.countUnreadMessagesRooms( userId);
    }
    public int getCountAllUnreadMessages(String userId) {
        return chatMessageRepository.countUnreadMessages(userId);
    }
    public  int getCountUnreadMessagesByRoomId(String roomId,String userId) {
        return chatMessageRepository.countUnreadMessagesByRoomId(roomId,userId);
    }

    public List<ChatMessageHistory> getRoomChattingHistory(String roomId) {
        return chatMessageHistoryRepository.findByRoomIdOrderByCreatedAt(roomId);

    }
}