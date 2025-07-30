package com.talktrip.talktrip.domain.member.enums;

public enum UserRole {
    U("일반 사용자"),
    A("관리자"),
    S("슈퍼 관리자");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
