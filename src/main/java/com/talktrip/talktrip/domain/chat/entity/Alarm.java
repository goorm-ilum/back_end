package com.talktrip.talktrip.domain.chat.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Alarm {
    @Id
    @GeneratedValue
    private Long id;

    private String accountEmail;         // 알림 받는 사용자
    private String message;        // 알림 내용
    private String link;           // 클릭 시 이동할 URI
    private Boolean isRead;
    private LocalDateTime createdAt;
}
