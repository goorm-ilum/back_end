package com.talktrip.talktrip.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
import com.talktrip.talktrip.domain.chat.entity.ChatMessage;
import com.talktrip.talktrip.domain.chat.entity.ChatRoom;
import com.talktrip.talktrip.domain.chat.repository.ChatMessageRepository;
import com.talktrip.talktrip.domain.chat.repository.ChatRoomMemberRepository;
import com.talktrip.talktrip.domain.chat.repository.ChatRoomRepository;
import com.talktrip.talktrip.global.redis.RedisPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate; // WebSocket 용
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RedisPublisher redisPublisher;
    private final ChannelTopic topic;//Spring Data Redis에서 Pub/Sub 구조에서 사용하는 "채널 이름"
    private final ObjectMapper objectMapper;
    private final ChatRoomMemberRepository chatRoomMemberRepository;



    public void saveAndSend(ChatMessageRequestDto dto) {
        // 1. DB 저장
        ChatMessage entity = chatMessageRepository.save(dto.toEntity());
        // 2. Redis 발행 → 직렬화는 RedisPublisher가 하도록 맡김
        redisPublisher.publish(topic, dto);
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

    public List<ChatMessage> getRoomChattingHistory(String roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAt(roomId);
    }
    @Transactional
    public List<ChatMessage> getRoomChattingHistoryAndMarkAsRead(String roomId,String memberId) {
        // 1. 메시지 조회
        List<ChatMessage> messages= chatMessageRepository.findByRoomIdOrderByCreatedAt(roomId);
        // 2. 읽음 처리 (chating_room_member_tab의 last_member_read_time update)
        chatRoomMemberRepository.updateLastReadTime("ROOM001", memberId);
        return messages;
    }
}