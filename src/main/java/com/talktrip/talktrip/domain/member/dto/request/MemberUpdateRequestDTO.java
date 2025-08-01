package com.talktrip.talktrip.domain.member.dto.request;

import com.talktrip.talktrip.domain.member.enums.Gender;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class MemberUpdateRequestDTO {

    private String name;
    private Gender gender;
    private LocalDate birthday;
    private String phoneNum;
}
