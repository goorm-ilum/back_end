package com.talktrip.talktrip.global.exception;

import lombok.Getter;

@Getter
public class ProductException extends RuntimeException {
    private final ErrorCode errorCode;

    public ProductException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

