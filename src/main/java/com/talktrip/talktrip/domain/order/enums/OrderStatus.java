package com.talktrip.talktrip.domain.order.enums;

public enum OrderStatus {
    PENDING,        // 결제대기
    SUCCESS,        // 결제완료 (예약확정)
    FAILED,         // 결제실패
    CANCELLED       // 예약취소
}
