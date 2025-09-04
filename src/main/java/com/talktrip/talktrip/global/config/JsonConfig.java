package com.talktrip.talktrip.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 직렬화 설정 클래스
 * 
 * 주요 기능:
 * 1. ObjectMapper 설정
 * 2. LocalDateTime 직렬화 지원
 * 3. Record 클래스 지원
 */
@Configuration
public class JsonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // record + LocalDateTime 직렬화 지원
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
