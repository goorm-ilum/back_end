package com.talktrip.talktrip.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
