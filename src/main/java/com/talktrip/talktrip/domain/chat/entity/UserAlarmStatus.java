package com.talktrip.talktrip.domain.chat.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class UserAlarmStatus {
    @Id
    private String userId;

    private int unreadAlarmCount;
}
