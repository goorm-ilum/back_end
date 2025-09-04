package com.talktrip.talktrip.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.chat.repository.ChatMessageRepository;
import com.talktrip.talktrip.domain.chat.repository.ChatRoomMemberRepository;
import com.talktrip.talktrip.domain.chat.repository.ChatRoomRepository;
import com.talktrip.talktrip.global.redis.RedisMessageBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionManager { 

    private final RedisTemplate<String,Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisMessageBroker redisMessageBroker;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    // 사용자 세션 관리: userId -> WebSocketSession Set
    private final ConcurrentMap<Long, java.util.Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    String serverRoomsKeyPrefix = "chat:room" ;
    String serverUsersKeyPrefix = "chat:user" ;
    @PostConstruct
    public void init() {
        log.info("WebSocketSessionManager 초기화 시작");
        
        // RedisMessageBroker에 로컬 메시지 핸들러 설정
        redisMessageBroker.setLocalMessageHandler(new RedisMessageBroker.LocalMessageHandler() {
            @Override
            public void handleLocalMessage(String topic, Object message, String messageId) {
                log.info("로컬 메시지 핸들러 호출 - Topic: {}, Message: {}, MessageId: {}", topic, message, messageId);
                // TODO: 채팅방에 있는 사용자들에게 메시지 전송 로직 구현
                if (message instanceof String) {
                    broadcastToRoom(topic, (String) message);
                }
            }
            
            @Override
            public boolean canHandleLocalMessage(String topic, Object message) {
                // 채팅방 관련 토픽만 처리
                return topic != null && topic.startsWith("chat:room:");
            }
        });
        
        log.info("WebSocketSessionManager 초기화 완료");
    }
    
    /**
     * 사용자 세션 추가
     * @param userId 사용자 ID
     * @param session WebSocket 세션
     */
    public void addSession(Long userId, WebSocketSession session) {
        log.info("사용자 세션 추가 - UserId: {}, SessionId: {}", userId, session.getId());
        
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        
        log.info("사용자 {}의 활성 세션 수: {}", userId, userSessions.get(userId).size());
    }
    
    /**
     * 사용자 세션 제거
     * @param userId 사용자 ID
     * @param session WebSocket 세션
     */
    public void removeSession(Long userId, WebSocketSession session) {
        //if 카카오톡 알림같이 ,지속적으로 발생하는 이벤트에 대한 session 추적을 해야한다면 session을 유지해야한다.
        log.info("사용자 세션 제거 - UserId: {}, SessionId: {}", userId, session.getId());
        
        java.util.Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            
            // 세션이 없으면 맵에서도 제거
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
                log.info("사용자 {}의 모든 세션이 제거됨", userId);
            } else {
                log.info("사용자 {}의 활성 세션 수: {}", userId, sessions.size());
            }
        }
        
        // 전체 연결된 사용자 수 계산
        int totalConnectedUsers = userSessions.values().stream()
                .mapToInt(Set::size)
                .sum();
        
        log.info("전체 연결된 사용자 수: {}", totalConnectedUsers);
        
        // 연결된 사용자가 0명이면 Redis 구독 해제 및 서버 정보 정리
        if (totalConnectedUsers == 0) {
            log.info("모든 사용자가 연결 해제됨. Redis 구독 및 서버 정보 정리 시작");
            
            // RedisMessageBroker에서 구독 중인 방 목록 가져오기
            Set<String> subscribedRooms = redisMessageBroker.getSubscribedRooms();
            
            if (!subscribedRooms.isEmpty()) {
                log.info("구독 중인 방 {}개 발견. 구독 해제 시작", subscribedRooms.size());
                
                // 각 방에서 구독 해제
                for (String roomId : subscribedRooms) {
                    if (roomId != null) {
                        log.info("방 {}에서 구독 해제", roomId);
                        redisMessageBroker.unsubscribeFromRoom(roomId);
                    }
                }
                
                log.info("모든 방에서 구독 해제 완료");
            } else {
                log.info("구독 중인 방이 없음");
            }
            
            // 서버 방 키 삭제 (Redis에 저장된 서버별 방 정보)
            String serverRoomsKey = serverRoomsKeyPrefix + ":" + redisMessageBroker.getInstanceId();
            Boolean deleted = redisTemplate.delete(serverRoomsKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("서버 방 키 삭제 완료: {}", serverRoomsKey);
            } else {
                log.info("서버 방 키가 이미 삭제됨: {}", serverRoomsKey);
            }
            
            // 서버 사용자 키도 삭제
            String serverUsersKey = serverUsersKeyPrefix + ":" + redisMessageBroker.getInstanceId();
            Boolean userKeyDeleted = redisTemplate.delete(serverUsersKey);
            if (Boolean.TRUE.equals(userKeyDeleted)) {
                log.info("서버 사용자 키 삭제 완료: {}", serverUsersKey);
            } else {
                log.info("서버 사용자 키가 이미 삭제됨: {}", serverUsersKey);
            }
            
            log.info("Redis 구독 및 서버 정보 정리 완료");
        }
    }
    /**
     * 사용자가 채팅방에 참여
     * 
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     */
    void joinRoom(Long userId, Long roomId) {
        String serverId = redisMessageBroker.getServerId();
        String serverRoomKey = serverRoomsKeyPrefix + ":" + serverId;
        
        // 이미 구독 중인지 확인
        Boolean wasAlreadySubscribed = redisTemplate.opsForSet().isMember(serverRoomKey, roomId.toString());
        
        // 구독하지 않은 경우에만 구독 추가
        if (!Boolean.TRUE.equals(wasAlreadySubscribed)) {
            redisMessageBroker.subscribeToRoom(roomId.toString());
        }
        
        // 서버별 방 정보에 방 ID 추가
        redisTemplate.opsForSet().add(serverRoomKey, roomId.toString());
        
        log.info("사용자 {}가 방 {}에 참여 - 서버: {}, 서버방키: {}", userId, roomId, serverId, serverRoomKey);
    }
    
    /**
     * 사용자의 모든 세션 가져오기
     * @param userId 사용자 ID
     * @return WebSocket 세션 Set
     */
    public java.util.Set<WebSocketSession> getUserSessions(Long userId) {
        return userSessions.getOrDefault(userId, ConcurrentHashMap.newKeySet());
    }
    
    /**
     * 사용자가 온라인인지 확인
     * @param userId 사용자 ID
     * @return 온라인 여부
     */
    public boolean isUserOnline(Long userId) {
        java.util.Set<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }
    
    /**
     * 특정 채팅방에 메시지 브로드캐스트
     */
    private void broadcastToRoom(String roomId, String message) {
        log.info("채팅방 {}에 메시지 브로드캐스트: {}", roomId, message);
        // TODO: 채팅방 멤버들에게 메시지 전송 로직 구현
    }

}
