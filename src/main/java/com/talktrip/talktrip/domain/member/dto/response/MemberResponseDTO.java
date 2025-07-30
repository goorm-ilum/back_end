package com.talktrip.talktrip.domain.member.dto.response;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.enums.Gender;
import com.talktrip.talktrip.domain.member.enums.UserRole;
import com.talktrip.talktrip.domain.member.enums.UserState;
import lombok.Getter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Getter
public class MemberResponseDTO {

    private final Long id;
    private final String accountEmail;
    private final String name;
    private final String nickname;
    private final UserRole userRole;
    private final Gender gender;
    private final LocalDate birthday;
    private final String profileImage;
    private final UserState userState;

    public MemberResponseDTO(Member member) {
        this.id = member.getUserId();
        this.accountEmail = member.getAccountEmail();
        this.name = member.getName();
        this.nickname = member.getNickname();
        this.userRole = member.getUserRole();
        this.gender = member.getGender();
        this.birthday = member.getBirthday();
        this.profileImage = member.getProfileImage();
        this.userState = member.getUserState();
    }

    // JWT 토큰 생성 시 claim에 넣을 정보 정리
    public Map<String, Object> getClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", id);
        claims.put("email", accountEmail);
        claims.put("name", name);
        claims.put("nickname", nickname);
        claims.put("role", userRole.name());
        claims.put("gender", gender.name());
        claims.put("birthday", birthday.toString());
        claims.put("profileImage", profileImage);
        claims.put("state", userState.name());
        return claims;
    }
}
