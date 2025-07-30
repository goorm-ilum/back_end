package com.talktrip.talktrip.domain.member.enums;

import lombok.Getter;

@Getter
public enum MemberRole {
    U("일반 사용자"),
    A("관리자"),
    S("슈퍼 관리자");

    private final String description;

    MemberRole(String description) {
        this.description = description;
    }

}
