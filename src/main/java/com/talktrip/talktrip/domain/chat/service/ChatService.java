package com.talktrip.talktrip.domain.chat.service;

import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
import com.talktrip.talktrip.domain.chat.dto.request.ChatRoomRequestDto;
import com.talktrip.talktrip.domain.chat.dto.response.*;
import com.talktrip.talktrip.domain.chat.entity.ChatMessage;
import com.talktrip.talktrip.domain.chat.entity.ChatRoom;
import com.talktrip.talktrip.domain.chat.entity.ChatRoomAccount;
import com.talktrip.talktrip.domain.chat.message.dto.ChatRoomUpdateMessage;
import com.talktrip.talktrip.domain.chat.message.dto.ChatUpdateMessage;
import com.talktrip.talktrip.domain.chat.repository.ChatMessageRepository;
import com.talktrip.talktrip.domain.chat.repository.ChatRoomMemberRepository;
import com.talktrip.talktrip.domain.chat.repository.ChatRoomRepository;
import com.talktrip.talktrip.global.dto.SliceResponse;
import com.talktrip.talktrip.global.redis.RedisPublisher;
import com.talktrip.talktrip.global.util.CursorUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChannelTopic topic;//Spring Data Redis에서 Pub/Sub 구조에서 사용하는 "채널 이름"
    private final ChannelTopic roomUpdateTopic;



    @Transactional
    public void saveAndSend(ChatMessageRequestDto dto,Principal principal) {
    try {
        String accountEmail = principal.getName();
        ChatMessage entity = chatMessageRepository.save(dto.toEntity(accountEmail));
        String receiverAccountEmail = chatMessageRepository.getOtherMemberIdByRoomIdandUserId(accountEmail, dto.getRoomId());
        int unreadCount = chatMessageRepository.countUnreadMessagesByRoomIdAndMemberId(dto.getRoomId(), accountEmail);


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

        //redisPublisher.publish(topic, updateMessage);
        //String destination = "/topic/chat/room/" +dto.getRoomId();
        messagingTemplate.convertAndSend("/topic/chat/room/", updateMessage);

        // 5) 실시간 전송: 단건 메시지 이벤트(채팅창 append 용)
        var messagePayload = ChatMessageResponseDto.from(entity);
        //redisPublisher.publish(roomUpdateTopic, updateMessage); //"/topic/chat/room/{roomId}/update"로 전달됨
        messagingTemplate.convertAndSend("/topic/chat/room/" +dto.getRoomId()+ "/update", updateMessage);

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


    @Transactional
    public SliceResponse<ChatMessageDto> getRoomChattingHistoryAndMarkAsRead(
            String roomId,
            String accountEmail,
            Integer limit,
            String cursor
    ) {
        // 1) page size 정규화
        final int size = (limit == null || limit <= 0 || limit > 200) ? 50 : limit;

        // 2) 정렬: 서버는 항상 최신→과거 (DESC)로 가져온다
        var sort = Sort.by(Sort.Direction.DESC, "createdAt", "messageId");
        var pageable = PageRequest.of(0, size, sort);

        // 3) 메시지 조회 (첫 진입 vs 커서 이전)
        List<ChatMessage> entities;
        if (cursor == null || cursor.isBlank()) {
            // 첫 페이지
            entities = chatMessageRepository.findFirstPage(roomId, pageable);
        } else {
            // 커서 이전 페이지
            var c = CursorUtil.decode(cursor); // createdAt + messageId(String)
            entities = chatMessageRepository.findSliceBefore(
                    roomId, c.createdAt(), c.messageId(), pageable
            );
        }

        // 4) 읽음 처리 (내 lastReadAt 갱신)
        chatRoomMemberRepository.updateLastReadTime(roomId, accountEmail);

        // 5) DTO 매핑
        var items = entities.stream().map(ChatMessageDto::from).toList();

        // 6) nextCursor/hasNext 계산
        String nextCursor = null;
        boolean hasNext = false;
        if (!entities.isEmpty()) {
            var last = entities.get(entities.size() - 1);
            nextCursor = CursorUtil.encode(last.getCreatedAt(), last.getMessageId());
            hasNext = (entities.size() == size); // 꽉 찼으면 더 있음으로 간주
        }

        return SliceResponse.of(items, hasNext ? nextCursor : null, hasNext);
    }

    // 기존 시그니처 유지용(호출부 점진 교체)
    @Transactional
    public SliceResponse<ChatMessageDto> getRoomChattingHistoryAndMarkAsRead(
            String roomId, String accountEmail
    ) {
        return getRoomChattingHistoryAndMarkAsRead(roomId, accountEmail, 50, null);
    }

    public void updateLastReadTime(String roomId, String accountEmail) {
        chatRoomMemberRepository.updateLastReadTime(roomId, accountEmail);
        // Redis로 업데이트 알림 발행 - roomId 포함하여 생성
        ChatUpdateMessage updateMessage = new ChatUpdateMessage(accountEmail);
        messagingTemplate.convertAndSend("/topic/chat/room/" +roomId+ "/update", updateMessage);
    }
    
    @Transactional
    public String enterOrCreateRoom(Principal principal, ChatRoomRequestDto chatRoomRequestDto) {
        String accountEmail = principal.getName();
        String sellerAccountEmail = chatRoomRequestDto.getSellerAccountEmail();
        Optional<String> existingRoom = chatRoomMemberRepository.findRoomIdByBuyerIdAndSellerId(accountEmail, sellerAccountEmail);

        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }
        ChatRoomResponseDto newRoomDto = ChatRoomResponseDto.createNew();
        String newRoomId = newRoomDto.getRoomId();

        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(newRoomId)
                .productId(chatRoomRequestDto.getProductId())
                .build();
        chatRoomRepository.save(chatRoom);

        ChatRoomAccount buyerMember = ChatRoomAccount.create(newRoomId, accountEmail);
        ChatRoomAccount sellerMember = ChatRoomAccount.create(newRoomId, sellerAccountEmail);

        chatRoomMemberRepository.save(buyerMember);
        chatRoomMemberRepository.save(sellerMember);

        return newRoomId;
    }
    @Transactional
    public void markChatRoomAsDeleted(String accountEmail, String roomId) {
        chatRoomMemberRepository.updateIsDelByMemberIdAndRoomId(accountEmail, roomId, 1);
    }
    public SliceResponse<ChatMessageDto> getRecentMessages(String roomId, Integer limit, String cursor) {
        int size = (limit == null || limit <= 0 || limit > 200) ? 50 : limit;

        var sort = Sort.by(Sort.Direction.DESC, "createdAt", "messageId");
        var pageable = PageRequest.of(0, size, sort);

        var messages = (cursor == null || cursor.isBlank())
                ? chatMessageRepository.findFirstPage(roomId, pageable)
                : loadBeforeCursor(roomId, size, cursor, pageable);

        // nextCursor 계산: 마지막 아이템 기준
        String nextCursor = null;
        boolean hasNext = false;
        if (!messages.isEmpty()) {
            var last = messages.get(messages.size() - 1);
            nextCursor = CursorUtil.encode(last.getCreatedAt(), last.getMessageId());

            // 다음 페이지가 더 있는지 가볍게 확인 (size 개수 꽉 찼으면 더 있다고 볼 수 있음)
            hasNext = messages.size() == size;
        }

        var items = messages.stream().map(ChatMessageDto::from).toList();
        return new SliceResponse<>(items, hasNext ? nextCursor : null, hasNext);
    }

    private List<ChatMessage> loadBeforeCursor(String roomId, int size, String cursor, PageRequest pageable) {
        var c = CursorUtil.decode(cursor);
        return chatMessageRepository.findSliceBefore(roomId, c.createdAt(), c.messageId(), pageable);
    }

    public ChatRoomDetailDto getRoomDetail(String roomId, String email) {
        var s = chatRoomRepository.findRoomScalar(roomId)
                .orElseThrow(() -> new IllegalArgumentException("room not found: " + roomId));

        // 추가 메타가 필요하면 다른 리포지토리에서 가져와 합쳐주세요.
        var myLastReadAt = chatRoomMemberRepository.findMyLastReadAt(roomId, email).orElse(null);
        var memberCount  = chatRoomMemberRepository.countMembers(roomId);
        var participants = chatRoomMemberRepository.findParticipantEmails(roomId);

        return new ChatRoomDetailDto(
                s.roomId(),      // 또는 s.getRoomId()
                s.title(),
                s.productId(),
                null,            // ownerEmail 필요시 r.roomAccountId도 JPQL에 추가하세요
                myLastReadAt,
                memberCount,
                participants
        );
    }
}