package com.talktrip.talktrip.domain.member.enums;

import lombok.Getter;

@Getter
public enum Gender {
    M("남성"),
    F("여성"),
    OTHER("기타");

    private final String description;

    Gender(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}