package com.talktrip.talktrip.global.exception;

import lombok.Getter;

@Getter
public class BuyerException extends RuntimeException {
    private final ErrorCode errorCode;

    public BuyerException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

