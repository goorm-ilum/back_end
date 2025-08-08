package com.talktrip.talktrip.global.exception;

import lombok.Getter;

@Getter
public class ProductException extends CustomException {
    public ProductException(ErrorCode errorCode) {
        super(errorCode);
    }
}

