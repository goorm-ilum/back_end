package com.talktrip.talktrip.domain.member.enums;

public enum UserState {
    A("활성 회원"),
    I("비활성 회원"),
    D("탈퇴 회원"),
    B("정지 회원");

    private final String description;

    UserState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
