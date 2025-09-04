package com.talktrip.talktrip.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

//redis의 메시지 순서 보장을 위한 서비스 (원자적인 연산)
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageSequenceService {
    private final RedisTemplate<String,Object> redisTemplate;
    private final String prefix = "chat:sequence";

    private Long getNextSequence(String roomId){
        String key = prefix+":"+roomId;
        
        // Redis의 원자적 증가 연산을 사용하여 메시지 순서를 보장
        Long sequence = redisTemplate.opsForValue().increment(key);
        
        // 만약 increment 결과가 null이면 1L을 반환
        return sequence != null ? sequence : 1L;
    }

    // ✅ 원자적 연산: 동시에 여러 메시지가 들어와도 순서 보장
    // ✅ Redis 기반: 빠른 성능과 확장성
    // ✅ 방별 분리: 각 채팅방마다 독립적인 순서 관리
    // ✅ 안전성: null 체크로 예외 상황 방지

}
