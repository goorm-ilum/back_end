package com.talktrip.talktrip.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.domain.chat.dto.response.ChatMessagePush;
import com.talktrip.talktrip.domain.chat.message.dto.ChatRoomUpdateMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.time.LocalDateTime;
import java.time.Duration;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Redis 메시지 브로커 서비스
 * 
 * RedisPublisher와 RedisPublisherWithRetry의 기능을 통합한 통합 메시지 브로커
 * 
 * 주요 기능:
 * 1. ConcurrentHashMap을 사용한 메시지 중복 처리 방지
 * 2. ReentrantLock을 사용한 동시 접근 제어
 * 3. AtomicLong을 사용한 메시지 ID 생성
 * 4. 메시지 처리 시간 추적 및 만료 관리
 * 5. 해시 기반 중복 메시지 방지
 * 6. 구독 중인 방 관리
 * 7. 주기적 정리 작업 스레드
 * 8. 단순 메시지 발행 (RedisPublisher 통합)
 * 9. 재시도 로직 포함 메시지 발행 (RedisPublisherWithRetry 통합)
 * 10. 트랜잭션 커밋 후 메시지 발행
 * 
 * 사용 예시:
 * // 단순 메시지 발행
 * redisMessageBroker.publish("chat:room:123", chatMessageDto);
 * 
 * // 중복 처리 포함 메시지 발행
 * String messageId = redisMessageBroker.publishMessage("chat:room:123", chatMessageDto);
 * 
 * // 채팅방 메시지 발행
 * redisMessageBroker.publishToRoom("123", chatMessageDto);
 * 
 * // 사용자 메시지 발행
 * redisMessageBroker.publishToUser("user@email.com", notificationDto);
 * 
 * // 트랜잭션 커밋 후 발행
 * redisMessageBroker.publishAfterCommit("chat:room:123", chatMessageDto);
 * 
 * // 재시도 로직 포함 발행
 * redisMessageBroker.publishWithRetry("chat:room:123", chatMessageDto);
 * 
 * // 트랜잭션 커밋 후 재시도 발행
 * redisMessageBroker.publishAfterCommitWithRetry("chat:room:123", chatMessageDto);
 */
@Slf4j
@Service
@Qualifier("redisSubscriber")
public class RedisMessageBroker implements MessageListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;
    
    // Redis 구독 관리자
    private RedisPubSubConfig.RedisSubscriptionManager subscriptionManager;
    
    // WebSocket 메시징 템플릿 (기존 RedisSubscriber 기능 통합)
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * -- GETTER --
     *  인스턴스 ID 조회
     * 
     * WebSocket 인스턴스를 식별하기 위한 고유 ID
     * 형태: hostname:port (예: localhost:8080)
     * 용도: 로그 추적, WebSocket 연결 관리, 멀티 인스턴스 환경에서 로그 구분
     *
     * @return 인스턴스 ID
     */
    @Getter
    private final String instanceId; // websocket 인스턴스 식별자

    /**
     * -- GETTER --
     * 서버 ID 조회
     * 
     * 서버 자체를 식별하기 위한 ID
     * 형태: HOSTNAME 환경 변수 또는 server-{timestamp}
     * 용도: Redis 메타데이터 관리, 서버 간 구분, 클러스터 환경에서 서버 식별
     *
     * @return 서버 ID
     */
    @Getter
    private final String serverId; // 서버 식별자 (HOSTNAME 또는 동적 생성)

    /**
     * 생성자
     * 
     * RedisMessageBroker의 모든 의존성을 주입받고 초기화합니다.
     * 
     * @param redisTemplate Redis 템플릿
     * @param objectMapper JSON 직렬화/역직렬화용 ObjectMapper
     * @param applicationContext Spring 애플리케이션 컨텍스트
     * @param messagingTemplate WebSocket 메시징 템플릿
     * @param instanceId WebSocket 인스턴스 식별자 (hostname:port 형태)
     */
    public RedisMessageBroker(RedisTemplate<String, Object> redisTemplate, 
                             ObjectMapper objectMapper, 
                             ApplicationContext applicationContext,
                             SimpMessagingTemplate messagingTemplate,
                             String instanceId) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.applicationContext = applicationContext;
        this.messagingTemplate = messagingTemplate;
        this.instanceId = instanceId;
        
        // 서버 ID 초기화: HOSTNAME 환경 변수 사용, 없으면 동적 생성
        // HOSTNAME 환경 변수가 설정되어 있으면 그 값을 사용하고,
        // 없으면 "server-{현재시간}" 형태로 동적 생성
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.trim().isEmpty()) {
            this.serverId = hostname;
        } else {
            this.serverId = "server-" + System.currentTimeMillis();
        }
        
        log.info("[RedisMessageBroker] 서버 ID 초기화: {}", this.serverId);
    }

    // 동시성 처리를 위한 필드들
    private final ConcurrentMap<String, MessageInfo> processedMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> messageHashToId = new ConcurrentHashMap<>(); // 해시 -> 메시지 ID
    private final ConcurrentMap<String, LocalDateTime> topicContentMap = new ConcurrentHashMap<>(); // 토픽+내용 -> 처리시간
    private final AtomicLong messageIdCounter = new AtomicLong(0);
    private final Lock messageProcessingLock = new ReentrantLock();
    
    // 구독 중인 방 관리 (ConcurrentHashMap.newKeySet() 사용) 중복된 곳을 구독하지 않으려는 의도
    private final Set<String> subscribedRooms = ConcurrentHashMap.newKeySet();
    private final Set<String> subscribedUsers = ConcurrentHashMap.newKeySet();


    // 주기적 작업을 위한 스케줄러
    private ScheduledExecutorService scheduler;

    // 메시지 만료 시간 (분)
    private static final int MESSAGE_EXPIRY_MINUTES = 30;
    // 중복 체크 시간 윈도우 (초) - 같은 내용의 메시지가 이 시간 내에 오면 중복으로 처리
    private static final int DUPLICATE_CHECK_WINDOW_SECONDS = 5;
    // 정리 작업 실행 주기 (분)
    private static final int CLEANUP_INTERVAL_MINUTES = 10;
    // 통계 로그 출력 주기 (분)
    private static final int STATS_LOG_INTERVAL_MINUTES = 5;

    // 메시지 처리 정보를 담는 내부 클래스
    private static class MessageInfo {
        private final String messageId;
        private final LocalDateTime processedAt;
        private final String topic;
        private final String content;
        private final String contentHash;

        public MessageInfo(String messageId, String topic, String content, String contentHash) {
            this.messageId = messageId;
            this.processedAt = LocalDateTime.now();
            this.topic = topic;
            this.content = content;
            this.contentHash = contentHash;
        }

        public boolean isExpired() {
            return Duration.between(processedAt, LocalDateTime.now()).toMinutes() > MESSAGE_EXPIRY_MINUTES;
        }

        @Override
        public String toString() {
            return String.format("MessageInfo{id=%s, topic=%s, hash=%s, processedAt=%s}",
                    messageId, topic, contentHash, processedAt);
        }
    }

    /**
     * DI 완료 후 최초 실행되는 초기화 메서드
     * 주기적 작업 스레드들을 시작합니다.
     */
    @PostConstruct
    public void init() {
        log.info("[RedisMessageBroker] 초기화 시작 - instanceId={}", instanceId);
        
        // ApplicationContext를 통해 RedisMessageListenerContainer 가져오기
        RedisMessageListenerContainer container = applicationContext.getBean(RedisMessageListenerContainer.class);
        
        // 패턴 기반 메시지 리스너 추가
        // chat:room:* 패턴: 채팅방 관련 모든 메시지 수신
        container.addMessageListener(this, new PatternTopic("chat:room:*"));
        log.info("[RedisMessageBroker] chat:room:* 패턴 구독 추가");
        
        // chat:user:* 패턴: 사용자 관련 모든 메시지 수신
        container.addMessageListener(this, new PatternTopic("chat:user:*"));
        log.info("[RedisMessageBroker] chat:user:* 패턴 구독 추가");
        
        // Redis 구독 관리자 초기화
        subscriptionManager = new RedisPubSubConfig.RedisSubscriptionManager(container, this);
        log.info("[RedisMessageBroker] Redis 구독 관리자 초기화 완료");
        
        // 스케줄러 생성 (데몬 스레드로 설정)
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "redis-broker-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        
        // 주기적 정리 작업 스케줄링
        scheduler.scheduleAtFixedRate(
            this::cleanupExpiredMessages,
            CLEANUP_INTERVAL_MINUTES, // 초기 지연
            CLEANUP_INTERVAL_MINUTES, // 실행 주기
            TimeUnit.MINUTES
        );
        
        // 주기적 통계 로그 출력 스케줄링
        scheduler.scheduleAtFixedRate(
            this::logStats,
            STATS_LOG_INTERVAL_MINUTES, // 초기 지연
            STATS_LOG_INTERVAL_MINUTES, // 실행 주기
            TimeUnit.MINUTES
        );
        
        // 주기적 구독 상태 체크 스케줄링
        scheduler.scheduleAtFixedRate(
            this::checkSubscriptionHealth,
            1, // 초기 지연 (1분)
            5, // 실행 주기 (5분)
            TimeUnit.MINUTES
        );
        
        log.info("[RedisMessageBroker] 초기화 완료 - 정리 작업({}분), 통계 로그({}분), 구독 체크(5분) 주기로 실행", 
                CLEANUP_INTERVAL_MINUTES, STATS_LOG_INTERVAL_MINUTES);
    }

    /**
     * 애플리케이션 종료 시 실행되는 정리 메서드
     */
    @PreDestroy
    public void destroy() {
        log.info("[RedisMessageBroker] 종료 시작");
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 마지막 정리 작업 실행
        cleanupExpiredMessages();
        
        log.info("[RedisMessageBroker] 종료 완료");
    }

    /**
     * 애플리케이션 종료 시 실행되는 정리 메서드 (PreDestroy 이벤트 감지)
     * 모든 리소스를 정리하고 메모리를 해제합니다.
     */
    @PreDestroy
    public void cleanup() {
        log.info("[RedisMessageBroker] cleanup() 메서드 실행 시작");
        
        try {
            // 1. 스케줄러 종료
            if (scheduler != null && !scheduler.isShutdown()) {
                log.info("[RedisMessageBroker] 스케줄러 종료 중...");
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                        log.warn("[RedisMessageBroker] 스케줄러 강제 종료");
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    log.warn("[RedisMessageBroker] 스케줄러 종료 중 인터럽트 발생");
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            // 2. 모든 구독 취소
            log.info("[RedisMessageBroker] 구독 취소 중...");
            int roomCount = subscribedRooms.size();
            int userCount = subscribedUsers.size();
            
            // Redis 구독 관리자를 통한 모든 구독 해제
            if (subscriptionManager != null) {
                subscriptionManager.unsubscribeAll();
            }
            
            // 모든 채팅방 구독 취소 (개별 채널 구독이 있는 경우)
            for (String roomId : subscribedRooms) {
                try {
                    if (subscriptionManager != null) {
                        subscriptionManager.unsubscribeFromRoomChannel(roomId);
                    }
                    log.debug("[RedisMessageBroker] 채팅방 구독 취소: {}", roomId);
                } catch (Exception e) {
                    log.warn("[RedisMessageBroker] 채팅방 구독 취소 실패: {}, 오류: {}", roomId, e.getMessage());
                }
            }
            
            // 모든 사용자 구독 취소 (개별 채널 구독이 있는 경우)
            for (String userId : subscribedUsers) {
                try {
                    subscriptionManager.unsubscribeFromUserChannel(userId);
                    log.debug("[RedisMessageBroker] 사용자 구독 취소: {}", userId);
                } catch (Exception e) {
                    log.warn("[RedisMessageBroker] 사용자 구독 취소 실패: {}, 오류: {}", userId, e.getMessage());
                }
            }
            
            // 3. 모든 메시지 정보 정리
            log.info("[RedisMessageBroker] 메시지 정보 정리 중...");
            int removedMessages = processedMessages.size();
            processedMessages.clear();
            
            // messageHashToId가 null이 아닌 경우에만 정리
            if (messageHashToId != null) {
                messageHashToId.clear();
            } else {
                log.warn("[RedisMessageBroker] messageHashToId가 null이므로 해시 맵 정리를 건너뜁니다.");
            }
            
            topicContentMap.clear();
            
            // 4. 구독 정보 정리
            log.info("[RedisMessageBroker] 구독 정보 정리 중...");
            subscribedRooms.clear();
            subscribedUsers.clear();
            
            // 5. 카운터 리셋
            messageIdCounter.set(0);
            
            log.info("[RedisMessageBroker] cleanup() 완료 - 구독취소(방: {}개, 사용자: {}개), 제거된 메시지: {}개", 
                    roomCount, userCount, removedMessages);
            
        } catch (Exception e) {
            log.error("[RedisMessageBroker] cleanup() 실행 중 오류 발생", e);
        } finally {
            log.info("[RedisMessageBroker] cleanup() 메서드 실행 완료");
        }
    }

    /**
     * 주기적 통계 로그 출력
     */
    private void logStats() {
        try {
            MessageStats messageStats = getMessageStats();
            SubscriptionStats subscriptionStats = getSubscriptionStats();
            
            log.info("[RedisMessageBroker] 주기적 통계 - 메시지: {}, 구독: {}", 
                    messageStats, subscriptionStats);
        } catch (Exception e) {
            log.error("[RedisMessageBroker] 통계 로그 출력 중 오류 발생", e);
        }
    }

    /**
     * 구독 상태 건강성 체크
     */
    private void checkSubscriptionHealth() {
        try {
            int roomCount = subscribedRooms.size();
            int userCount = subscribedUsers.size();
            
            // 구독 상태가 비정상적으로 많은 경우 경고
            if (roomCount > 1000) {
                log.warn("[RedisMessageBroker] 구독 중인 채팅방이 많음: {}개", roomCount);
            }
            
            if (userCount > 10000) {
                log.warn("[RedisMessageBroker] 구독 중인 사용자가 많음: {}명", userCount);
            }
            
            // 구독 상태가 비어있는 경우 정보 로그
            if (roomCount == 0 && userCount == 0) {
                log.debug("[RedisMessageBroker] 현재 구독 중인 방/사용자 없음");
            }
            
        } catch (Exception e) {
            log.error("[RedisMessageBroker] 구독 상태 체크 중 오류 발생", e);
        }
    }

    /**
     * 메시지를 발행하고 중복 처리 방지
     * 
     * @param topic 메시지 토픽
     * @param message 메시지 내용
     * @return 메시지 ID
     */
    public String publishMessage(String topic, Object message) {
        messageProcessingLock.lock();
        try {
            // 메시지 내용을 JSON으로 직렬화
            String messageContent = objectMapper.writeValueAsString(message);
            
            // 메시지 해시 생성
            String messageHash = generateMessageHash(topic, messageContent);
            
            // messageHashToId가 null인 경우 처리
            if (messageHashToId == null) {
                log.warn("[RedisMessageBroker] messageHashToId가 null입니다. 중복 체크를 건너뜁니다.");
            }
            
            // 중복 처리 확인 (여러 방법으로 체크)
            if (messageHashToId != null && isDuplicateMessage(topic, messageContent, messageHash)) {
                String existingTimeBasedId = messageHashToId.get(messageHash);
                String existingId = extractMessageIdFromTimeBasedId(existingTimeBasedId);
                log.warn("[RedisMessageBroker] 중복 메시지 감지: topic={}, hash={}, existingId={}", 
                        topic, messageHash, existingId);
                return existingId != null ? existingId : generateMessageId();
            }
            
            // 메시지 ID 생성
            String messageId = generateMessageId();
            
            // Redis에 메시지 발행
            redisTemplate.convertAndSend(topic, messageContent);
            
            // 처리된 메시지 정보 저장
            MessageInfo messageInfo = new MessageInfo(messageId, topic, messageContent, messageHash);
            processedMessages.put(messageId, messageInfo);
            
            // messageHashToId가 null이 아닌 경우에만 저장
            if (messageHashToId != null) {
                // 현재 시간을 ID에 포함하여 저장 (정렬을 위해)
                long currentTime = System.currentTimeMillis();
                String timeBasedId = currentTime + "_" + messageId;
                messageHashToId.put(messageHash, timeBasedId);
                
                // messageHashToId 크기가 10000을 넘으면 오래된 데이터 정리
                if (messageHashToId.size() > 10000) {
                    cleanupOldMessageHashes();
                }
            } else {
                log.warn("[RedisMessageBroker] messageHashToId가 null이므로 해시 저장을 건너뜁니다: hash={}", messageHash);
            }
            
            // 토픽+내용 기반 중복 체크를 위한 키 저장
            String topicContentKey = topic + ":" + messageHash;
            topicContentMap.put(topicContentKey, LocalDateTime.now());
            
            log.info("[RedisMessageBroker] 메시지 발행 완료: messageId={}, topic={}, hash={}", 
                    messageId, topic, messageHash);
            
            return messageId;
            
        } catch (Exception e) {
            log.error("[RedisMessageBroker] 메시지 발행 실패: topic={}, error={}", topic, e.getMessage(), e);
            throw new RuntimeException("메시지 발행 실패", e);
        } finally {
            messageProcessingLock.unlock();
        }
    }

    /**
     * 단순 메시지 발행 (중복 처리 없음)
     * RedisPublisher의 기능을 통합
     * 
     * @param channel Redis 채널
     * @param payload 메시지 내용
     */
    public void publish(String channel, Object payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend(channel, message);
            log.info("[RedisMessageBroker] 단순 메시지 발행 완료: channel={}, message={}", channel, message);
        } catch (Exception e) {
            log.error("[RedisMessageBroker] 단순 메시지 발행 실패: channel={}, error={}", channel, e.getMessage(), e);
            throw new RuntimeException("단순 메시지 발행 실패", e);
        }
    }

    /**
     * 채팅방에 메시지 발행 (RedisPublisher.publishToRoom 통합)
     * 
     * @param roomId 채팅방 ID
     * @param dto 채팅 메시지 DTO
     */
    public void publishToRoom(String roomId, Object dto) {
        try {
            String channel = "chat:room:" + roomId;
            String message = objectMapper.writeValueAsString(dto);
            redisTemplate.convertAndSend(channel, message);
            log.info("[RedisMessageBroker] 채팅방 메시지 발행 완료: roomId={}, channel={}", roomId, channel);
        } catch (Exception e) {
            log.error("[RedisMessageBroker] 채팅방 메시지 발행 실패: roomId={}, error={}", roomId, e.getMessage(), e);
            throw new RuntimeException("채팅방 메시지 발행 실패", e);
        }
    }

    /**
     * 사용자에게 메시지 발행
     * 
     * @param userId 사용자 ID
     * @param dto 메시지 DTO
     */
    public void publishToUser(String userId, Object dto) {
        try {
            String channel = "chat:user:" + userId;
            String message = objectMapper.writeValueAsString(dto);
            redisTemplate.convertAndSend(channel, message);
            log.info("[RedisMessageBroker] 사용자 메시지 발행 완료: userId={}, channel={}", userId, channel);
        } catch (Exception e) {
            log.error("[RedisMessageBroker] 사용자 메시지 발행 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("사용자 메시지 발행 실패", e);
        }
    }

    /**
     * 트랜잭션 커밋 후 메시지 발행
     * RedisPublisher.publishAfterCommit 통합
     * 
     * @param channel Redis 채널
     * @param payload 메시지 내용
     */
    public void publishAfterCommit(String channel, Object payload) {
        runAfterCommit(() -> publish(channel, payload));
    }

    /**
     * 트랜잭션 커밋 후 채팅방 메시지 발행
     * 
     * @param roomId 채팅방 ID
     * @param dto 메시지 DTO
     */
    public void publishToRoomAfterCommit(String roomId, Object dto) {
        runAfterCommit(() -> publishToRoom(roomId, dto));
    }

    /**
     * 트랜잭션 커밋 후 사용자 메시지 발행
     * 
     * @param userId 사용자 ID
     * @param dto 메시지 DTO
     */
    public void publishToUserAfterCommit(String userId, Object dto) {
        runAfterCommit(() -> publishToUser(userId, dto));
    }

    /**
     * 트랜잭션 커밋 후 실행되는 작업 등록
     * 
     * @param task 실행할 작업
     */
    private void runAfterCommit(Runnable task) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override 
                        public void afterCommit() { 
                            task.run(); 
                        }
                    }
            );
        } else {
            task.run();
        }
    }

    /**
     * 재시도 로직을 포함한 메시지 발행 (RedisPublisherWithRetry 통합)
     * 
     * @param channel Redis 채널
     * @param message 메시지 내용
     * @param maxAttempts 최대 재시도 횟수
     * @param initialBackoff 초기 백오프 시간 (ms)
     * @param backoffMultiplier 백오프 배수
     * @param maxBackoff 최대 백오프 시간 (ms)
     */
    public void publishWithRetry(String channel, Object message, int maxAttempts, long initialBackoff, 
                                double backoffMultiplier, long maxBackoff) {
        int attempts = 0;
        long backoff = initialBackoff;

        while (attempts < maxAttempts) {
            try {
                 String jsonMessage = objectMapper.writeValueAsString(message);
                 redisTemplate.convertAndSend(channel, jsonMessage);
                 log.debug("[RedisMessageBroker] 재시도 메시지 발행 성공 - 시도 횟수: {}", attempts + 1);
                return;
            } catch (Exception e) {
                attempts++;

                if (attempts >= maxAttempts) {
                    log.error("[RedisMessageBroker] 최대 재시도 횟수 초과. 메시지 발행 실패: channel={}, message={}", 
                            channel, message, e);
                    throw new RuntimeException("Redis 메시지 발행 실패", e);
                }

                log.warn("[RedisMessageBroker] 메시지 발행 실패, {}ms 후 재시도 ({}/{}): channel={}", 
                        backoff, attempts, maxAttempts, channel);

                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                }

                // 다음 재시도를 위한 backoff 시간 계산
                backoff = Math.min(
                        (long) (backoff * backoffMultiplier),
                        maxBackoff
                );
            }
        }
    }

    /**
     * 기본 설정으로 재시도 메시지 발행
     * 
     * @param channel Redis 채널
     * @param message 메시지 내용
     */
    public void publishWithRetry(String channel, Object message) {
        publishWithRetry(channel, message, 3, 1000L, 2.0, 10000L);
    }

    /**
     * 트랜잭션 커밋 후 재시도 메시지 발행
     * 
     * @param channel Redis 채널
     * @param message 메시지 내용
     */
    public void publishAfterCommitWithRetry(String channel, Object message) {
        runAfterCommit(() -> publishWithRetry(channel, message));
    }

    /**
     * 트랜잭션 커밋 후 재시도 채팅방 메시지 발행
     * 
     * @param roomId 채팅방 ID
     * @param dto 메시지 DTO
     */
    public void publishToRoomAfterCommitWithRetry(String roomId, Object dto) {
        runAfterCommit(() -> {
            try {
                String channel = "chat:room:" + roomId;
                publishWithRetry(channel, dto);
            } catch (Exception e) {
                log.error("[RedisMessageBroker] 채팅방 재시도 메시지 발행 실패: roomId={}", roomId, e);
            }
        });
    }

    /**
     * 트랜잭션 커밋 후 재시도 사용자 메시지 발행
     * 
     * @param userId 사용자 ID
     * @param dto 메시지 DTO
     */
    public void publishToUserAfterCommitWithRetry(String userId, Object dto) {
        runAfterCommit(() -> {
            try {
                String channel = "chat:user:" + userId;
                publishWithRetry(channel, dto);
            } catch (Exception e) {
                log.error("[RedisMessageBroker] 사용자 재시도 메시지 발행 실패: userId={}", userId, e);
            }
        });
    }

    /**
     * 채팅방 구독 추가
     * 
     * @param roomId 채팅방 ID
     * @return 구독 성공 여부
     */
    public boolean subscribeToRoom(String roomId) {

//         실제 Redis 구독을 추가하는 것이 아님 (이미 패턴 기반으로 모든 방 메시지 수신 중)
// 애플리케이션 레벨에서 관심 있는 방을 추적하는 역할
// 중복 구독 방지 및 구독 상태 관리
// 통계 수집 및 모니터링을 위한 데이터 제공
// 메모리 효율성을 위한 구독 상태 추적
// 이는 비즈니스 로직 레벨의 구독 관리와 Redis Pub/Sub 레벨의 구독을 분리하여 관리하는 설계입니다!
        boolean added = subscribedRooms.add(roomId);// Set이므로 중복 방지
        if (added) {
            log.info("[RedisMessageBroker] 채팅방 구독 추가: roomId={}", roomId);
        } else {
            log.debug("[RedisMessageBroker] 이미 구독 중인 채팅방: roomId={}", roomId);
        }
        return added;
    }

    /**
     * 채팅방 구독 해제
     * 
     * @param roomId 채팅방 ID
     * @return 구독 해제 성공 여부
     */
    public boolean unsubscribeFromRoom(String roomId) {
//         실제 Redis Pub/Sub 구독/해제가 아님
// 애플리케이션 레벨의 구독 상태 관리
// 메모리 효율성과 통계를 위한 추적
// 비즈니스 로직에서 관심 있는 방만 관리
// 이는 Redis Pub/Sub 레벨과 애플리케이션 비즈니스 로직 레벨을 분리하여 관리하는 깔끔한 설계입니다! 🎯
        boolean removed = subscribedRooms.remove(roomId);
        if (removed) {
            log.info("[RedisMessageBroker] 채팅방 구독 해제: roomId={}", roomId);
        } else {
            log.debug("[RedisMessageBroker] 구독하지 않은 채팅방: roomId={}", roomId);
        }
        return removed;
    }

    /**
     * 사용자 구독 추가
     * 
     * @param userId 사용자 ID
     * @return 구독 성공 여부
     */
    public boolean subscribeUser(String userId) {
        boolean added = subscribedUsers.add(userId);
        if (added) {
            log.info("[RedisMessageBroker] 사용자 구독 추가: userId={}", userId);
        } else {
            log.debug("[RedisMessageBroker] 이미 구독 중인 사용자: userId={}", userId);
        }
        return added;
    }

    /**
     * 사용자 구독 해제
     * 
     * @param userId 사용자 ID
     * @return 구독 해제 성공 여부
     */
    public boolean unsubscribeUser(String userId) {
        boolean removed = subscribedUsers.remove(userId);
        if (removed) {
            log.info("[RedisMessageBroker] 사용자 구독 해제: userId={}", userId);
        } else {
            log.debug("[RedisMessageBroker] 구독하지 않은 사용자: userId={}", userId);
        }
        return removed;
    }

    /**
     * 구독 중인 채팅방 목록 조회
     * 
     * @return 구독 중인 채팅방 ID Set
     */
    public Set<String> getSubscribedRooms() {
        return Set.copyOf(subscribedRooms);
    }

    /**
     * 구독 중인 사용자 목록 조회
     * 
     * @return 구독 중인 사용자 ID Set
     */
    public Set<String> getSubscribedUsers() {
        return Set.copyOf(subscribedUsers);
    }

    /**
     * 특정 채팅방 구독 여부 확인
     * 
     * @param roomId 채팅방 ID
     * @return 구독 중인지 여부
     */
    public boolean isSubscribedToRoom(String roomId) {
        return subscribedRooms.contains(roomId);
    }

    /**
     * 특정 사용자 구독 여부 확인
     * 
     * @param userId 사용자 ID
     * @return 구독 중인지 여부
     */
    public boolean isUserSubscribed(String userId) {
        return subscribedUsers.contains(userId);
    }

    /**
     * 구독 통계 정보 조회
     * 
     * @return 구독 통계 정보
     */
    public SubscriptionStats getSubscriptionStats() {
        return new SubscriptionStats(
                subscribedRooms.size(),
                subscribedUsers.size(),
                getSubscribedRooms(),
                getSubscribedUsers()
        );
    }

    /**
     * 구독 통계 정보 클래스
     */
    public static class SubscriptionStats {
        private final int subscribedRoomCount;
        private final int subscribedUserCount;
        private final Set<String> subscribedRooms;
        private final Set<String> subscribedUsers;
        
        public SubscriptionStats(int subscribedRoomCount, int subscribedUserCount, 
                               Set<String> subscribedRooms, Set<String> subscribedUsers) {
            this.subscribedRoomCount = subscribedRoomCount;
            this.subscribedUserCount = subscribedUserCount;
            this.subscribedRooms = subscribedRooms;
            this.subscribedUsers = subscribedUsers;
        }
        
        public int getSubscribedRoomCount() { return subscribedRoomCount; }
        public int getSubscribedUserCount() { return subscribedUserCount; }
        public Set<String> getSubscribedRooms() { return subscribedRooms; }
        public Set<String> getSubscribedUsers() { return subscribedUsers; }
        
        @Override
        public String toString() {
            return String.format("SubscriptionStats{rooms=%d, users=%d}", 
                    subscribedRoomCount, subscribedUserCount);
        }
    }

    /**
     * 중복 메시지인지 확인 (여러 방법으로 체크)
     * 
     * @param topic 토픽
     * @param content 메시지 내용
     * @param messageHash 메시지 해시
     * @return 중복 메시지인지 여부
     */
    private boolean isDuplicateMessage(String topic, String content, String messageHash) {
        // messageHashToId가 null인 경우 중복 체크 불가
        if (messageHashToId == null) {
            log.warn("[RedisMessageBroker] messageHashToId가 null이므로 중복 체크를 건너뜁니다.");
            return false;
        }
        
        // 1. 해시 기반 중복 체크
        if (messageHashToId.containsKey(messageHash)) {
            String existingTimeBasedId = messageHashToId.get(messageHash);
            String existingId = extractMessageIdFromTimeBasedId(existingTimeBasedId);
            MessageInfo existingInfo = processedMessages.get(existingId);
            if (existingInfo != null && !existingInfo.isExpired()) {
                return true;
            } else {
                // 만료된 메시지 정보 정리
                messageHashToId.remove(messageHash);
                if (existingId != null) {
                    processedMessages.remove(existingId);
                }
            }
        }
        
        // 2. 토픽+내용 기반 중복 체크 (시간 윈도우 내)
        String topicContentKey = topic + ":" + messageHash;
        LocalDateTime lastProcessed = topicContentMap.get(topicContentKey);
        if (lastProcessed != null) {
            Duration timeDiff = Duration.between(lastProcessed, LocalDateTime.now());
            if (timeDiff.getSeconds() < DUPLICATE_CHECK_WINDOW_SECONDS) {
                log.debug("[RedisMessageBroker] 시간 윈도우 내 중복 메시지: topic={}, timeDiff={}s", 
                        topic, timeDiff.getSeconds());
                return true;
            }
        }
        
        // 3. 기존 메시지 ID 기반 중복 체크
        return isMessageAlreadyProcessed(generateMessageId(), topic, content);
    }

    /**
     * 메시지 해시 생성 (SHA-256 사용)
     * 
     * @param topic 토픽
     * @param content 메시지 내용
     * @return 해시값
     */
    private String generateMessageHash(String topic, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String dataToHash = topic + ":" + content;
            byte[] hashBytes = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("[RedisMessageBroker] 해시 생성 실패", e);
            // SHA-256이 없는 경우 대체 방법 사용
            return String.valueOf((topic + ":" + content).hashCode());
        }
    }

    /**
     * 메시지가 이미 처리되었는지 확인 (기존 방식)
     * 
     * @param messageId 메시지 ID
     * @param topic 토픽
     * @param content 메시지 내용
     * @return 이미 처리된 메시지인지 여부
     */
    private boolean isMessageAlreadyProcessed(String messageId, String topic, String content) {
        MessageInfo existingInfo = processedMessages.get(messageId);
        
        if (existingInfo != null) {
            // 만료된 메시지 정보 제거
            if (existingInfo.isExpired()) {
                processedMessages.remove(messageId);
                return false;
            }
            
            // 동일한 토픽과 내용인지 확인
            return existingInfo.topic.equals(topic) && existingInfo.content.equals(content);
        }
        
        return false;
    }

    /**
     * 고유한 메시지 ID 생성
     * 
     * @return 메시지 ID
     */
    private String generateMessageId() {
        long id = messageIdCounter.incrementAndGet();
        return String.format("msg_%d_%d", System.currentTimeMillis(), id);
    }

    /**
     * 만료된 메시지 정보 정리
     */
    public void cleanupExpiredMessages() {
        try {
            // messageHashToId가 null인 경우 처리 불가
            if (messageHashToId == null) {
                log.warn("[RedisMessageBroker] messageHashToId가 null이므로 정리 작업을 건너뜁니다.");
                return;
            }
            
            // 만료된 메시지 정보 제거
            int removedMessages = 0;
            removedMessages = processedMessages.entrySet().removeIf(entry -> {
                boolean expired = entry.getValue().isExpired();
                if (expired) {
                    log.debug("[RedisMessageBroker] 만료된 메시지 제거: {}", entry.getValue());
                    // 해시 맵에서도 제거
                    if (messageHashToId != null) {
                        messageHashToId.remove(entry.getValue().contentHash);
                    }
                }
                return expired;
            }) ? 1 : 0;
            
            // 만료된 토픽+내용 키 제거
            int removedKeys = 0;
            removedKeys = topicContentMap.entrySet().removeIf(entry -> {
                boolean expired = Duration.between(entry.getValue(), LocalDateTime.now()).getSeconds() > DUPLICATE_CHECK_WINDOW_SECONDS;
                if (expired) {
                    log.debug("[RedisMessageBroker] 만료된 토픽+내용 키 제거: {}", entry.getKey());
                }
                return expired;
            }) ? 1 : 0;
            
            if (removedMessages > 0 || removedKeys > 0) {
                log.info("[RedisMessageBroker] 정리 작업 완료 - 메시지: {}개, 키: {}개", removedMessages, removedKeys);
            }
            
        } catch (Exception e) {
            log.error("[RedisMessageBroker] 정리 작업 중 오류 발생", e);
        }
    }

    /**
     * 1분 이상 된 처리된 메시지 정보 정리
     * 
     * @return 제거된 메시지 개수
     */
    public int cleanUpProcessedMessage() {
        try {
            // messageHashToId가 null인 경우 처리 불가
            if (messageHashToId == null) {
                log.warn("[RedisMessageBroker] messageHashToId가 null이므로 1분 이상 된 메시지 정리를 건너뜁니다.");
                return 0;
            }
            
            LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
            int removedCount = 0;
            
            // 1분 이상 된 메시지 정보 제거
            removedCount = processedMessages.entrySet().removeIf(entry -> {
                boolean oldMessage = entry.getValue().processedAt.isBefore(oneMinuteAgo);
                if (oldMessage) {
                    log.debug("[RedisMessageBroker] 1분 이상 된 메시지 제거: {}", entry.getValue());
                    // 해시 맵에서도 제거
                    if (messageHashToId != null) {
                        messageHashToId.remove(entry.getValue().contentHash);
                    }
                }
                return oldMessage;
            }) ? processedMessages.size() : 0;
            
            // 1분 이상 된 토픽+내용 키 제거
            int removedKeys = 0;
            removedKeys = topicContentMap.entrySet().removeIf(entry -> {
                boolean oldKey = entry.getValue().isBefore(oneMinuteAgo);
                if (oldKey) {
                    log.debug("[RedisMessageBroker] 1분 이상 된 토픽+내용 키 제거: {}", entry.getKey());
                }
                return oldKey;
            }) ? topicContentMap.size() : 0;
            
            if (removedCount > 0 || removedKeys > 0) {
                log.info("[RedisMessageBroker] 1분 이상 된 메시지 정리 완료 - 메시지: {}개, 키: {}개", removedCount, removedKeys);
            }
            
            return removedCount;
            
        } catch (Exception e) {
            log.error("[RedisMessageBroker] 1분 이상 된 메시지 정리 중 오류 발생", e);
            return 0;
        }
    }

    /**
     * Kotlin 코드를 Java로 변환한 메시지 정리 메서드
     * 1분 이상 된 처리된 메시지를 정리합니다.
     */
    private void cleanUpProcessedMessages() {
        try {
            long now = System.currentTimeMillis();
            
            // 1분(60000ms) 이상 된 메시지 키들을 찾아서 제거
            Set<String> expiredKeys = processedMessages.entrySet().stream()
                    .filter(entry -> {
                        long messageTime = entry.getValue().processedAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                        return now - messageTime > 60000; // 1분
                    })
                    .map(Map.Entry::getKey)
                    .collect(java.util.stream.Collectors.toSet());
            
            // 만료된 키들을 제거
            expiredKeys.forEach(processedMessages::remove);
            
            if (!expiredKeys.isEmpty()) {
                log.info("[RedisMessageBroker] Redis에서 {}개의 메시지 제거됨", expiredKeys.size());
            }
            
        } catch (Exception e) {
            log.error("[RedisMessageBroker] cleanUpProcessedMessages 실행 중 오류 발생", e);
        }
    }

    /**
     * 처리된 메시지 통계 정보 조회
     * 
     * @return 메시지 통계 정보
     */
    public MessageStats getMessageStats() {
        // messageHashToId가 null인 경우 기본값 사용
        long uniqueHashes = messageHashToId != null ? messageHashToId.size() : 0;
        
        long totalMessages = processedMessages.size();
        long expiredMessages = processedMessages.values().stream()
                .filter(MessageInfo::isExpired)
                .count();
        long topicContentKeys = topicContentMap.size();
        
        return new MessageStats(totalMessages, expiredMessages, messageIdCounter.get(), uniqueHashes, topicContentKeys);
    }

    /**
     * 메시지 통계 정보 클래스
     */
    public static class MessageStats {
        private final long totalMessages;
        private final long expiredMessages;
        private final long totalGeneratedIds;
        private final long uniqueHashes;
        private final long topicContentKeys;
        
        public MessageStats(long totalMessages, long expiredMessages, long totalGeneratedIds, 
                          long uniqueHashes, long topicContentKeys) {
            this.totalMessages = totalMessages;
            this.expiredMessages = expiredMessages;
            this.totalGeneratedIds = totalGeneratedIds;
            this.uniqueHashes = uniqueHashes;
            this.topicContentKeys = topicContentKeys;
        }
        
        public long getTotalMessages() { return totalMessages; }
        public long getExpiredMessages() { return expiredMessages; }
        public long getTotalGeneratedIds() { return totalGeneratedIds; }
        public long getActiveMessages() { return totalMessages - expiredMessages; }
        public long getUniqueHashes() { return uniqueHashes; }
        public long getTopicContentKeys() { return topicContentKeys; }
        
        @Override
        public String toString() {
            return String.format("MessageStats{total=%d, expired=%d, active=%d, generated=%d, hashes=%d, keys=%d}", 
                    totalMessages, expiredMessages, getActiveMessages(), totalGeneratedIds, uniqueHashes, topicContentKeys);
        }
    }

    /**
     * 특정 메시지 ID의 처리 정보 조회
     * 
     * @param messageId 메시지 ID
     * @return 메시지 정보 (없으면 null)
     */
    public MessageInfo getMessageInfo(String messageId) {
        MessageInfo info = processedMessages.get(messageId);
        if (info != null && info.isExpired()) {
            processedMessages.remove(messageId);
            // messageHashToId가 null이 아닌 경우에만 제거
            if (messageHashToId != null) {
                messageHashToId.remove(info.contentHash);
            }
            return null;
        }
        return info;
    }

    /**
     * 해시로 메시지 정보 조회
     * 
     * @param messageHash 메시지 해시
     * @return 메시지 정보 (없으면 null)
     */
    public MessageInfo getMessageInfoByHash(String messageHash) {
        // messageHashToId가 null인 경우 처리 불가
        if (messageHashToId == null) {
            log.warn("[RedisMessageBroker] messageHashToId가 null이므로 해시로 메시지 정보 조회를 건너뜁니다.");
            return null;
        }
        
        String timeBasedId = messageHashToId.get(messageHash);
        if (timeBasedId != null) {
            String messageId = extractMessageIdFromTimeBasedId(timeBasedId);
            return getMessageInfo(messageId);
        }
        return null;
    }

    /**
     * 모든 처리된 메시지 정보 조회 (디버깅용)
     * 
     * @return 처리된 메시지 맵의 복사본
     */
    public ConcurrentMap<String, MessageInfo> getAllProcessedMessages() {
        return new ConcurrentHashMap<>(processedMessages);
    }

    /**
     * 중복 체크 맵 정보 조회 (디버깅용)
     * 
     * @return 해시 맵의 복사본
     */
    public ConcurrentMap<String, String> getMessageHashMap() {
        // messageHashToId가 null인 경우 빈 맵 반환
        if (messageHashToId == null) {
            log.warn("[RedisMessageBroker] messageHashToId가 null이므로 빈 맵을 반환합니다.");
            return new ConcurrentHashMap<>();
        }
        
        return new ConcurrentHashMap<>(messageHashToId);
    }

    /**
     * 로컬 메시지 핸들러 설정 (역의존성을 위한 메서드)
     * 
     * @param handler 로컬 메시지 핸들러
     */
    public void setLocalMessageHandler(LocalMessageHandler handler) {
        if (handler != null) {
            log.info("[RedisMessageBroker] 로컬 메시지 핸들러 설정: {}", handler.getClass().getSimpleName());
            this.localMessageHandler = handler;
        } else {
            log.warn("[RedisMessageBroker] 로컬 메시지 핸들러가 null입니다");
            this.localMessageHandler = null;
        }
    }

    /**
     * MessageListener 인터페이스 구현
     * Redis에서 수신된 메시지를 처리합니다.
     * 
     * @param message Redis에서 수신된 메시지
     * @param pattern 메시지 패턴 (패턴 구독인 경우)
     */
    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            
            log.info("[{}][RedisMessageBroker] 메시지 수신: channel={}, pattern={}, payloadLength={}", 
                    instanceId, channel, pattern != null ? new String(pattern) : "null", payload.length());
            
            // 이중 직렬화된 문자열 처리 (기존 RedisSubscriber 로직)
            if (payload.startsWith("\"") && payload.endsWith("\"")) {
                payload = objectMapper.readValue(payload, String.class);
                log.debug("[{}][RedisMessageBroker] 이중 직렬화 해제 완료: payloadLength={}", instanceId, payload.length());
            }
            
            // 메시지 해시 생성
            String messageHash = generateMessageHash(channel, payload);
            
            // messageHashToId가 null인 경우 초기화
            if (messageHashToId == null) {
                log.warn("[{}][RedisMessageBroker] messageHashToId가 null입니다. 초기화를 진행합니다.", instanceId);
                // ConcurrentHashMap으로 초기화 (이미 final로 선언되어 있으므로 재할당 불가)
                // 대신 안전하게 처리
                log.info("[{}][RedisMessageBroker] messageHashToId null 처리 완료", instanceId);
            }
            
            // 중복 메시지 체크 - 이미 처리된 메시지인지 확인
            if (messageHashToId != null && messageHashToId.containsKey(messageHash)) {
                String existingTimeBasedId = messageHashToId.get(messageHash);
                String existingId = extractMessageIdFromTimeBasedId(existingTimeBasedId);
                MessageInfo existingInfo = processedMessages.get(existingId);
                
                if (existingInfo != null && !existingInfo.isExpired()) {
                    log.warn("[{}][RedisMessageBroker] 중복 메시지 무시: channel={}, hash={}, existingId={}", 
                            instanceId, channel, messageHash, existingId);
                    return; // 중복 메시지는 처리하지 않고 종료
                } else {
                    // 만료된 메시지 정보 정리
                    log.debug("[{}][RedisMessageBroker] 만료된 메시지 정보 정리: hash={}", instanceId, messageHash);
                    messageHashToId.remove(messageHash);
                    if (existingId != null) {
                        processedMessages.remove(existingId);
                    }
                }
            }
            
            // 메시지 ID 생성 및 처리 정보 저장
            String messageId = generateMessageId();
            MessageInfo messageInfo = new MessageInfo(messageId, channel, payload, messageHash);
            processedMessages.put(messageId, messageInfo);
            
            // messageHashToId가 null이 아닌 경우에만 저장
            if (messageHashToId != null) {
                // 현재 시간을 ID에 포함하여 저장 (정렬을 위해)
                long currentTime = System.currentTimeMillis();
                String timeBasedId = currentTime + "_" + messageId;
                messageHashToId.put(messageHash, timeBasedId);
                
                // messageHashToId 크기가 10000을 넘으면 오래된 데이터 정리
                if (messageHashToId.size() > 10000) {
                    cleanupOldMessageHashes();
                }
            } else {
                log.warn("[{}][RedisMessageBroker] messageHashToId가 null이므로 해시 저장을 건너뜁니다: hash={}", 
                        instanceId, messageHash);
            }
            
            // 채널 타입에 따른 처리
            broadcastToRoom(channel, payload);
            
        } catch (Exception e) {
            log.error("[{}][RedisMessageBroker] 메시지 처리 중 오류 발생: {}", instanceId, e.getMessage(), e);
        }
    }
    
    /**
     * 채널 타입에 따른 메시지 브로드캐스트 처리
     * 
     * @param channel 채널명
     * @param payload 메시지 내용
     */
    private void broadcastToRoom(String channel, String payload) {
        if (channel.startsWith("chat:room:")) {
            handleRoomMessage(channel, payload);
        } else if (channel.startsWith("chat:user:")) {
            handleUserMessage(channel, payload);
        } else {
            handleGeneralMessage(channel, payload);
        }
    }
    
    /**
     * 채팅방 메시지 처리
     * 
     * @param channel 채널명
     * @param messageBody 메시지 내용
     */
    private void handleRoomMessage(String channel, String messageBody) {
        try {
            // 채널에서 방 ID 추출
            String roomId = channel.substring("chat:room:".length());
            
            log.info("[{}][RedisMessageBroker] 채팅방 메시지 처리: roomId={}", instanceId, roomId);
            
            // 메시지를 JSON으로 파싱하여 객체로 변환
            Object messageObject = objectMapper.readValue(messageBody, Object.class);
            
            // 로컬 메시지 핸들러가 설정되어 있다면 호출
            if (localMessageHandler != null && localMessageHandler.canHandleLocalMessage(channel, messageObject)) {
                String messageId = generateMessageId();
                localMessageHandler.handleLocalMessage(channel, messageObject, messageId);
            }
            
            // 기존 RedisSubscriber 기능: WebSocket으로 클라이언트에게 전송
            try {
                // ChatMessagePush DTO로 파싱 시도
                ChatMessagePush dto =
                    objectMapper.readValue(messageBody,ChatMessagePush.class);
                String dest = "/topic/chat/room/" + dto.getRoomId();    // 프론트 구독 경로
                messagingTemplate.convertAndSend(dest, dto);
                log.info("[{}][RedisMessageBroker] WebSocket 전송 완료 -> dest={}, msgId={}", 
                        instanceId, dest, dto.getMessageId());
            } catch (Exception e) {
                log.warn("[{}][RedisMessageBroker] ChatMessagePush 파싱 실패, 일반 메시지로 처리: {}", 
                        instanceId, e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("[{}][RedisMessageBroker] 채팅방 메시지 처리 실패: channel={}, error={}", 
                    instanceId, channel, e.getMessage(), e);
        }
    }
    
    /**
     * 사용자 메시지 처리
     * 
     * @param channel 채널명
     * @param messageBody 메시지 내용
     */
    private void handleUserMessage(String channel, String messageBody) {
        try {
            // 채널에서 사용자 ID 추출
            String userId = channel.substring("chat:user:".length());
            
            log.info("[{}][RedisMessageBroker] 사용자 메시지 처리: userId={}", instanceId, userId);
            
            // 메시지를 JSON으로 파싱하여 객체로 변환
            Object messageObject = objectMapper.readValue(messageBody, Object.class);
            
            // 로컬 메시지 핸들러가 설정되어 있다면 호출
            if (localMessageHandler != null && localMessageHandler.canHandleLocalMessage(channel, messageObject)) {
                String messageId = generateMessageId();
                localMessageHandler.handleLocalMessage(channel, messageObject, messageId);
            }
            
            // 기존 RedisSubscriber 기능: 개인 사이드바 업데이트
            try {
                // ChatRoomUpdateMessage DTO로 파싱 시도
                ChatRoomUpdateMessage dto =
                    objectMapper.readValue(messageBody, ChatRoomUpdateMessage.class);
                messagingTemplate.convertAndSendToUser(userId, "/queue/chat/rooms", dto);
                log.info("[{}][RedisMessageBroker] 개인 메시지 전송 완료 -> user={}, dest=/queue/chat/rooms", 
                        instanceId, userId);
            } catch (Exception e) {
                log.warn("[{}][RedisMessageBroker] ChatRoomUpdateMessage 파싱 실패, 일반 메시지로 처리: {}", 
                        instanceId, e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("[{}][RedisMessageBroker] 사용자 메시지 처리 실패: channel={}, error={}", 
                    instanceId, channel, e.getMessage(), e);
        }
    }
    
    /**
     * 일반 메시지 처리
     * 
     * @param channel 채널명
     * @param messageBody 메시지 내용
     */
    private void handleGeneralMessage(String channel, String messageBody) {
        try {
            log.info("[{}][RedisMessageBroker] 일반 메시지 처리: channel={}", instanceId, channel);
            
            // 메시지를 JSON으로 파싱하여 객체로 변환
            Object messageObject = objectMapper.readValue(messageBody, Object.class);
            
            // 로컬 메시지 핸들러가 설정되어 있다면 호출
            if (localMessageHandler != null && localMessageHandler.canHandleLocalMessage(channel, messageObject)) {
                String messageId = generateMessageId();
                localMessageHandler.handleLocalMessage(channel, messageObject, messageId);
            }
            
            // 여기에 추가적인 일반 메시지 처리 로직을 구현할 수 있습니다
            
        } catch (Exception e) {
            log.error("[{}][RedisMessageBroker] 일반 메시지 처리 실패: channel={}, error={}", 
                    instanceId, channel, e.getMessage(), e);
        }
    }
    
    /**
     * 오래된 메시지 해시 정리 (크기 제한)
     * messageHashToId 크기가 10000을 넘을 때 호출되어 오래된 데이터를 제거합니다.
     */
    private void cleanupOldMessageHashes() {
        try {
            if (messageHashToId == null || messageHashToId.size() <= 10000) {
                return; // 정리할 필요 없음
            }
            
            log.info("[{}][RedisMessageBroker] messageHashToId 크기 제한 정리 시작: 현재 크기={}", 
                    instanceId, messageHashToId.size());
            
            // 시간 기반으로 정렬하여 오래된 데이터 찾기
            List<Map.Entry<String, String>> sortedEntries = messageHashToId.entrySet().stream()
                    .sorted((e1, e2) -> {
                        try {
                            // timeBasedId 형식: "timestamp_messageId"
                            long time1 = Long.parseLong(e1.getValue().split("_")[0]);
                            long time2 = Long.parseLong(e2.getValue().split("_")[0]);
                            return Long.compare(time1, time2); // 오름차순 정렬 (오래된 것부터)
                        } catch (Exception ex) {
                            // 파싱 실패 시 기본 정렬
                            return e1.getValue().compareTo(e2.getValue());
                        }
                    })
                    .collect(Collectors.toList());
            
            // 가장 오래된 20% 제거 (최소 1000개, 최대 2000개)
            int totalSize = messageHashToId.size();
            int removeCount = Math.min(Math.max(totalSize / 5, 1000), 2000); // 20% 또는 1000-2000개
            
            int removed = 0;
            for (int i = 0; i < removeCount && i < sortedEntries.size(); i++) {
                Map.Entry<String, String> entry = sortedEntries.get(i);
                String removedHash = entry.getKey();
                String removedTimeBasedId = entry.getValue();
                
                // messageHashToId에서 제거
                if (messageHashToId.remove(removedHash) != null) {
                    removed++;
                    
                    // processedMessages에서도 해당 메시지 제거
                    try {
                        String messageId = removedTimeBasedId.split("_", 2)[1]; // messageId 부분 추출
                        processedMessages.remove(messageId);
                    } catch (Exception ex) {
                        log.debug("[{}][RedisMessageBroker] 메시지 ID 파싱 실패: {}", instanceId, removedTimeBasedId);
                    }
                }
            }
            
            log.info("[{}][RedisMessageBroker] messageHashToId 크기 제한 정리 완료: 제거된 항목={}개, 남은 크기={}", 
                    instanceId, removed, messageHashToId.size());
            
        } catch (Exception e) {
            log.error("[{}][RedisMessageBroker] messageHashToId 크기 제한 정리 중 오류 발생: {}", 
                    instanceId, e.getMessage(), e);
        }
    }
    
    /**
     * timeBasedId에서 messageId 추출
     * timeBasedId 형식: "timestamp_messageId"
     * 
     * @param timeBasedId 시간 기반 ID
     * @return messageId
     */
    private String extractMessageIdFromTimeBasedId(String timeBasedId) {
        try {
            if (timeBasedId == null) {
                return null;
            }
            // "_"로 분리하여 messageId 부분 추출
            String[] parts = timeBasedId.split("_", 2);
            if (parts.length >= 2) {
                return parts[1]; // messageId 부분
            } else {
                // 기존 형식인 경우 그대로 반환
                return timeBasedId;
            }
        } catch (Exception e) {
            log.debug("[RedisMessageBroker] timeBasedId 파싱 실패: {}", timeBasedId);
            return timeBasedId; // 파싱 실패 시 원본 반환
        }
    }

    /**
     * 로컬 메시지 핸들러 인터페이스
     */
    public interface LocalMessageHandler {
        /**
         * 로컬 메시지 처리
         * 
         * @param topic 메시지 토픽
         * @param message 메시지 내용
         * @param messageId 메시지 ID
         */
        void handleLocalMessage(String topic, Object message, String messageId);
        
        /**
         * 로컬 메시지 처리 성공 여부 확인
         * 
         * @param topic 메시지 토픽
         * @param message 메시지 내용
         * @return 처리 성공 여부
         */
        boolean canHandleLocalMessage(String topic, Object message);
    }
    
    // 로컬 메시지 핸들러 필드 추가
    private LocalMessageHandler localMessageHandler;
}
