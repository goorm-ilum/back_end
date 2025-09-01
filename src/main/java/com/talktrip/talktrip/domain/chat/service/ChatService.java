package com.talktrip.talktrip.domain.chat.service;

import com.talktrip.talktrip.domain.chat.dto.request.ChatMessageRequestDto;
import com.talktrip.talktrip.domain.chat.dto.request.ChatRoomRequestDto;
import com.talktrip.talktrip.domain.chat.dto.response.*;
import com.talktrip.talktrip.domain.chat.dto.response.ChatMessagePush;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.Principal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final RedisPublisher redisPublisher;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;


    @Transactional
    public void saveAndSend(ChatMessageRequestDto dto, Principal principal) {
        try {
            // Redis 연결 상태 미리 확인 (메시지 저장 전에 체크)
            if (!isRedisAvailable()) {
                log.error("Redis 연결이 불가능합니다.");
                throw new RuntimeException("Redis 서버에 연결할 수 없습니다.");
            }
            final String sender = principal.getName();
            
            // 테스트용: 특정 메시지로 에러 발생 (실제 운영에서는 제거)
            if (dto.getMessage() != null && dto.getMessage().contains("테스트에러")) {
                throw new RuntimeException("테스트용 에러: 메시지에 '테스트에러'가 포함되어 있습니다.");
            }

            // (선택) 권한 체크
            // if (!chatRoomMemberRepository.existsByRoomIdAndAccountEmail(dto.getRoomId(), sender)) {
            //     throw new AccessDeniedException("Not a member of this room");
            // }

            // 1) DB 저장
            ChatMessage entity = chatMessageRepository.save(dto.toEntity(sender));
            
            // 2) ChatRoom의 updatedAt 업데이트 (최신 메시지 시간으로)
            chatRoomRepository.updateUpdatedAt(dto.getRoomId(), entity.getCreatedAt());

            // 2) 방 브로드캐스트 payload
            ChatMessagePush push = ChatMessagePush.builder()
                    .messageId(entity.getMessageId())
                    .roomId(entity.getRoomId())
                    .sender(sender)
                    .senderName(sender.split("@")[0])
                    .message(entity.getMessage())
                    .createdAt(String.valueOf(entity.getCreatedAt()))
                    .build();

            // 3) 개인 사이드바 payload들 미리 계산 (트랜잭션 안에서 조회 OK)
            List<String> memberEmails = chatRoomMemberRepository
                    .findAllAccountEmailsByRoomId(dto.getRoomId())
                    .stream().map(ChatRoomAccount::getAccountEmail).toList();

            List<ChatRoomUpdateMessage> sidebars = new ArrayList<>(memberEmails.size());
            for (String email : memberEmails) {
                int unreadForThisUser = email.equals(sender)
                        ? 0
                        : chatMessageRepository.countUnreadMessagesByRoomIdAndMemberId(dto.getRoomId(), email);

                sidebars.add(ChatRoomUpdateMessage.builder()
                        .roomId(dto.getRoomId())
                        .messageId(entity.getMessageId())
                        .message(entity.getMessage())
                        .senderAccountEmail(sender)
                        .createdAt(entity.getCreatedAt())
                        .unreadCountForReceiver(unreadForThisUser)
                        .updatedAt(Timestamp.valueOf(LocalDateTime.now()))
                        .build());
            }

            // 4) ❗ DB 커밋이 "성공한 뒤에만" Redis로 팬아웃
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    publishToRedis(dto, push, memberEmails, sidebars);
                }
            });

        } catch (AccessDeniedException e) {
            log.error("채팅방 접근 권한 없음: {}", e.getMessage(), e);
            throw new RuntimeException("채팅방에 접근할 권한이 없습니다.");
        } catch (IllegalArgumentException e) {
            log.error("잘못된 메시지 데이터: {}", e.getMessage(), e);
            throw new RuntimeException("잘못된 메시지 형식입니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("채팅 메시지 저장 및 발행 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("채팅 처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        // (선택) 기타 후처리
        chatRoomMemberRepository.resetIsDelByRoomId(dto.getRoomId());
    }

    /**
     * Redis로 채팅 메시지와 사이드바 업데이트를 발행합니다.
     * DB 커밋 후 실행되므로 예외가 발생해도 트랜잭션에 영향을 주지 않습니다.
     */
    private void publishToRedis(ChatMessageRequestDto dto, ChatMessagePush push, 
                               List<String> memberEmails, List<ChatRoomUpdateMessage> sidebars) {
        try {
            // 방 전체 브로드캐스트 → 모든 WS 서버가 이 채널을 구독 중
            redisPublisher.publish("chat:room:" + dto.getRoomId(), push);

            // 개인별 사이드바 업데이트 → 각 사용자 채널로 발행
            publishSidebarUpdates(memberEmails, sidebars);
            
        } catch (Exception e) {
            log.error("Redis 발행 실패: {}", e.getMessage(), e);
            // afterCommit에서는 예외를 던져도 트랜잭션이 이미 커밋되어 WebSocket 컨트롤러에서 잡히지 않음
            // 대신 로그만 남기고, 클라이언트는 메시지가 성공적으로 저장되었다고 생각할 수 있음
            // 실제 운영에서는 Redis 모니터링이나 알림 시스템을 통해 처리해야 함
        }
    }

    /**
     * 각 사용자별로 사이드바 업데이트 메시지를 Redis에 발행합니다.
     */
    private void publishSidebarUpdates(List<String> memberEmails, List<ChatRoomUpdateMessage> sidebars) {
        for (int i = 0; i < memberEmails.size(); i++) {
            String email = memberEmails.get(i);
            ChatRoomUpdateMessage sidebar = sidebars.get(i);
            redisPublisher.publish("chat:user:" + email, sidebar);
        }
    }

    /**
     * Redis 연결 상태 확인
     */
    private boolean isRedisAvailable() {
        try {
            // Redis 연결 상태를 간단한 명령으로 확인
            // 연결이 안되면 예외 발생
            stringRedisTemplate.opsForValue().get("health_check");
            return true;
        } catch (Exception e) {
            log.warn("Redis 연결 확인 실패: {}", e.getMessage());
            return false;
        }
    }
    public String createRoom(String userA, String userB) {
        // 기존 방 있으면 재사용, 없으면 새로 생성
        // room_id = UUID.ranㄴdomUUID().toString()
        return userA;
    }
    public List<ChatRoomDTO> getRooms(String accountEmail) {
        //redis 추가
        List<ChatRoomDTO> rooms = chatRoomRepository.findRoomsWithLastMessageByMemberId(accountEmail);
        
        // updatedAt 내림차순으로 정렬 (최신 메시지 순)
        rooms.sort((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()));
        
        return rooms;
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
    public SliceResponse<ChatMemberRoomWithMessageDto> getRoomChattingHistoryAndMarkAsRead(
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
        var items = entities.stream()
                .map(ChatMemberRoomWithMessageDto::from) // ChatMessage -> ChatMemberRoomWithMessageDto 매핑
                .toList();


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
//    @Transactional
//    public SliceResponse<ChatMemberRoomWithMessageDto> getRoomChattingHistoryAndMarkAsRead(
//            String roomId, String accountEmail
//    ) {
//        return getRoomChattingHistoryAndMarkAsRead(roomId, accountEmail, 50, null);
//    }

//    public void updateLastReadTime(String roomId, String accountEmail) {
//        chatRoomMemberRepository.updateLastReadTime(roomId, accountEmail);
//        // Redis로 업데이트 알림 발행 - roomId 포함하여 생성
//        ChatUpdateMessage updateMessage = new ChatUpdateMessage(accountEmail);
//        messagingTemplate.convertAndSend("/topic/chat/room/" +roomId+ "/update", updateMessage);
//    }
    
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
    public ChatRoomDetailDto getRoomDetail(String roomId, String email) {
        var s = chatRoomRepository.findRoomScalar(roomId)
                .orElseThrow(() -> new IllegalArgumentException("room not found: " + roomId));

        // 추가 메타가 필요하면 다른 리포지토리에서 가져와 합쳐주세요.
        var roomUpdatedAt = chatRoomRepository.findChatRoomUpdateAtByRoomId(roomId);
        var memberCount  = chatRoomMemberRepository.countMembers(roomId);
        var participants = chatRoomMemberRepository.findParticipantEmails(roomId);

        return new ChatRoomDetailDto(
                s.roomId(),      // 또는 s.getRoomId()
                s.title(),
                s.productId(),
                null,            // ownerEmail 필요시 r.roomAccountId도 JPQL에 추가하세요
                roomUpdatedAt,
                memberCount,
                participants
        );
    }
}