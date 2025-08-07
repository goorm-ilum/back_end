package com.talktrip.talktrip.domain.chat.service;

import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
import com.talktrip.talktrip.domain.chat.dto.response.ChatRoomResponseDto;
import com.talktrip.talktrip.domain.chat.entity.ChatMessage;
import com.talktrip.talktrip.domain.chat.entity.ChatRoom;
import com.talktrip.talktrip.domain.chat.entity.ChatRoomMember;
import com.talktrip.talktrip.domain.chat.message.dto.ChatRoomUpdateMessage;
import com.talktrip.talktrip.domain.chat.message.dto.ChatUpdateMessage;
import com.talktrip.talktrip.domain.chat.repository.ChatMessageRepository;
import com.talktrip.talktrip.domain.chat.repository.ChatRoomMemberRepository;
import com.talktrip.talktrip.domain.chat.repository.ChatRoomRepository;
import com.talktrip.talktrip.global.redis.RedisPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RedisPublisher redisPublisher;

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChannelTopic topic;//Spring Data Redis에서 Pub/Sub 구조에서 사용하는 "채널 이름"
    private final ChannelTopic roomUpdateTopic;


    @Transactional
    public void saveAndSend(ChatMessageRequestDto dto) {
        // 1. DB 저장
        ChatMessage entity = chatMessageRepository.save(dto.toEntity());
        // 2. Redis 발행
        int unreadCount = chatMessageRepository.countUnreadMessagesByRoomIdAndMemberId(dto.getRoomId(), dto.getReceiverId());

        // 3. Redis로 상대방에게 "해당 방만 업데이트" 메시지 전송
        ChatRoomUpdateMessage updateMessage = ChatRoomUpdateMessage.builder()
                .memberId(dto.getMemberId())
                .receiverId(dto.getReceiverId())
                .message(entity.getMessage())
                .roomId(dto.getRoomId())
                .notReadMessageCount(unreadCount)
                .updatedAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        redisPublisher.publish(topic, updateMessage);

    }
    public String createRoom(String userA, String userB) {
        // 기존 방 있으면 재사용, 없으면 새로 생성
        // room_id = UUID.randomUUID().toString()
        return userA;
    }
    public List<ChatRoom> getRooms(String memberId) {
        //redis 추가
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
    public List<ChatMessage> getRoomChattingHistoryAndMarkAsRead(String roomId, String memberId) {
        // 1. 메시지 조회
        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderByCreatedAt(roomId);
        // 2. 읽음 처리
        chatRoomMemberRepository.updateLastReadTime(roomId, memberId);
        return messages;
    }

    public void updateLastReadTime(String roomId, String memberId) {
        chatRoomMemberRepository.updateLastReadTime(roomId, memberId);
        // Redis로 업데이트 알림 발행 - roomId 포함하여 생성
        ChatUpdateMessage updateMessage = new ChatUpdateMessage(memberId);
        redisPublisher.publish(roomUpdateTopic, updateMessage);
    }
    
    @Transactional
    public String enterOrCreateRoom(String buyerId, String sellerId) {

        Optional<String> existingRoom = chatRoomMemberRepository.findRoomIdByBuyerIdAndSellerId(buyerId, sellerId);

        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }
        ChatRoomResponseDto newRoomDto = ChatRoomResponseDto.createNew();
        String newRoomId = newRoomDto.getRoomId();

        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(newRoomId)
                .build();
        chatRoomRepository.save(chatRoom);

        ChatRoomMember buyerMember = ChatRoomMember.create(newRoomId, buyerId);
        ChatRoomMember sellerMember = ChatRoomMember.create(newRoomId, sellerId);

        chatRoomMemberRepository.save(buyerMember);
        chatRoomMemberRepository.save(sellerMember);

        return newRoomId;
    }
}