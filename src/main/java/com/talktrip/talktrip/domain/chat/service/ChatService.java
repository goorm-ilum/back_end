package com.talktrip.talktrip.domain.chat.service;

import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
import com.talktrip.talktrip.domain.chat.dto.response.ChatMessageResponseDto;
import com.talktrip.talktrip.domain.chat.dto.response.ChatRoomDTO;
import com.talktrip.talktrip.domain.chat.dto.response.ChatRoomResponseDto;
import com.talktrip.talktrip.domain.chat.entity.ChatMessage;
import com.talktrip.talktrip.domain.chat.entity.ChatRoom;
import com.talktrip.talktrip.domain.chat.entity.ChatRoomAccount;
import com.talktrip.talktrip.domain.chat.message.dto.ChatRoomUpdateMessage;
import com.talktrip.talktrip.domain.chat.message.dto.ChatUpdateMessage;
import com.talktrip.talktrip.domain.chat.repository.ChatMessageRepository;
import com.talktrip.talktrip.domain.chat.repository.ChatRoomMemberRepository;
import com.talktrip.talktrip.domain.chat.repository.ChatRoomRepository;
import com.talktrip.talktrip.global.redis.RedisPublisher;
import com.talktrip.talktrip.global.util.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import java.security.Principal;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Slf4j
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
    public void saveAndSend(ChatMessageRequestDto dto,Principal principal) {
    try {
        String accountEmail = dto.getAccountEmail();
        ChatMessage entity = chatMessageRepository.save(dto.toEntity());
        String receiverAccountEmail = chatMessageRepository.getOtherMemberIdByRoomIdandUserId(accountEmail, dto.getRoomId());
        int unreadCount = chatMessageRepository.countUnreadMessagesByRoomIdAndMemberId(dto.getRoomId(), accountEmail);//수정하기....


        // 3) 사용자별 안읽음 수 계산
        int unreadCountForReceiver = chatMessageRepository.countUnreadMessagesByRoomIdAndMemberId(
                dto.getRoomId(), receiverAccountEmail
        );
        int unreadCountForSender = chatMessageRepository.countUnreadMessagesByRoomIdAndMemberId(
                dto.getRoomId(), accountEmail
        );


        ChatRoomUpdateMessage updateMessage = ChatRoomUpdateMessage.builder()
                .accountEmail(accountEmail)
                .receiverAccountEmail(receiverAccountEmail)
                .message(entity.getMessage())
                .roomId(dto.getRoomId())
                .notReadMessageCount(unreadCount)
                .unreadCountForSender(unreadCountForSender)
                .unreadCountForReceiver(unreadCountForReceiver)
                .updatedAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        redisPublisher.publish(topic, updateMessage);


        // 5) 실시간 전송: 단건 메시지 이벤트(채팅창 append 용)
        //var messagePayload = ChatMessageResponseDto.from(entity);
        redisPublisher.publish(roomUpdateTopic, updateMessage); //"/topic/chat/room/{roomId}/update"로 전달됨


    chatRoomMemberRepository.resetIsDelByRoomId(dto.getRoomId());

    } catch (Exception e) {
        log.error("채팅 메시지 저장 및 발행 중 오류 발생: {}", e.getMessage());
        throw new RuntimeException("채팅 처리 중 오류가 발생했습니다.");
    }
}
    public String createRoom(String userA, String userB) {
        // 기존 방 있으면 재사용, 없으면 새로 생성
        // room_id = UUID.ranㄴdomUUID().toString()
        return userA;
    }
    public List<ChatRoomDTO> getRooms(String accountEmail) {
        //redis 추가
        String memberId = SecurityUtils.currentUserId();

        return chatRoomRepository.findRoomsWithLastMessageByMemberId(accountEmail);
    }
    public int getCountALLUnreadMessagesRooms(String accountEmail) {
        return chatMessageRepository.countUnreadMessagesRooms( accountEmail);
    }
    public int getCountAllUnreadMessages(String accountEmail) {
        return chatMessageRepository.countUnreadMessages(accountEmail);
    }
    public  int getCountUnreadMessagesByRoomId(String roomId,String accountEmail) {
        return chatMessageRepository.countUnreadMessagesByRoomId(roomId,accountEmail);
    }

    public List<ChatMessage> getRoomChattingHistory(String roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAt(roomId);
    }
    @Transactional
    public List<ChatMessage> getRoomChattingHistoryAndMarkAsRead(String roomId, String accountEmail) {
        // 1. 메시지 조회
        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderByCreatedAt(roomId);
        // 2. 읽음 처리
        chatRoomMemberRepository.updateLastReadTime(roomId, accountEmail);
        return messages;
    }

    public void updateLastReadTime(String roomId, String accountEmail) {
        chatRoomMemberRepository.updateLastReadTime(roomId, accountEmail);
        // Redis로 업데이트 알림 발행 - roomId 포함하여 생성
        ChatUpdateMessage updateMessage = new ChatUpdateMessage(accountEmail);
        redisPublisher.publish(roomUpdateTopic, updateMessage);
    }
    
    @Transactional
    public String enterOrCreateRoom(String buyerId, String sellerId) {
        String accountEmail = SecurityUtils.currentAccountEmail();
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

        ChatRoomAccount buyerMember = ChatRoomAccount.create(newRoomId, buyerId);
        ChatRoomAccount sellerMember = ChatRoomAccount.create(newRoomId, sellerId);

        chatRoomMemberRepository.save(buyerMember);
        chatRoomMemberRepository.save(sellerMember);

        return newRoomId;
    }
    @Transactional
    public void markChatRoomAsDeleted(String accountEmail, String roomId) {
        chatRoomMemberRepository.updateIsDelByMemberIdAndRoomId(accountEmail, roomId, 1);
    }
}