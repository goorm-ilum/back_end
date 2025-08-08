package com.talktrip.talktrip.global.exception;

import lombok.Getter;

@Getter
public class MemberException extends CustomException {
    public MemberException(ErrorCode errorCode) {
        super(errorCode);
    }
}

