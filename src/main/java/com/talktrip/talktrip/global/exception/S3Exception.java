package com.talktrip.talktrip.global.exception;

import lombok.Getter;

@Getter
public class S3Exception extends CustomException {
    public S3Exception(ErrorCode errorCode) {
        super(errorCode);
    }
}

