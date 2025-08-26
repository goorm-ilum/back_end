package com.talktrip.talktrip.global.exception;

import lombok.Getter;

@Getter
public class OrderException extends CustomException {
    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }
}