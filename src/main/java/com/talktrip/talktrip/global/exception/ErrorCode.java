package com.talktrip.talktrip.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "판매자를 찾을 수 없습니다."),

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    COUNTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "국가를 찾을 수 없습니다."),

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 리뷰를 작성하셨습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_PRODUCT_MISMATCH(HttpStatus.BAD_REQUEST, "해당 주문에서 상품 정보를 찾을 수 없습니다."),
    ORDER_EMPTY(HttpStatus.BAD_REQUEST, "주문에 상품이 존재하지 않습니다."),
    ORDER_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "완료된 주문만 리뷰 작성이 가능합니다."),

    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3 이미지 업로드에 실패했습니다."),
    IMAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3 이미지 삭제에 실패했습니다."),

    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요 정보를 찾을 수 없습니다."),
    LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 좋아요를 누른 상품입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
