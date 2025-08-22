package com.talktrip.talktrip.global.exception;

import com.talktrip.talktrip.global.exception.CustomException;

public class LikeException extends CustomException {
    public LikeException(ErrorCode errorCode) {
        super(errorCode);
    }
}
