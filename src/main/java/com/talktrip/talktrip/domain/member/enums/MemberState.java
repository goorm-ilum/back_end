package com.talktrip.talktrip.domain.member.enums;

import lombok.Getter;

@Getter
public enum MemberState {
    A("활성 회원"),
    I("비활성 회원"),
    D("탈퇴 회원"),
    B("정지 회원");

    private final String description;

    MemberState(String description) {
        this.description = description;
    }

}