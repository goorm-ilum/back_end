package com.talktrip.talktrip.global.exception;

import lombok.Getter;

@Getter
public class S3Excepttion extends RuntimeException {
    private final ErrorCode errorCode;

    public S3Excepttion(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

