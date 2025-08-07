package com.talktrip.talktrip.domain.order.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class OrderRequestDTO {

    private String date;     // 선택한 날짜 (예: "2025-08-02")
    private List<Option> options;  // 선택 옵션과 수량 리스트
    private int totalPrice; // 총 결제 금액

    @Getter
    @Setter
    public static class Option {
        private Long productOptionId; // 옵션 ID 추가
        private String optionName; // 옵션 이름
        private int quantity;      // 선택 수량
        private int price; // 주문 당시 가격
        private int discountPrice; // 주문 당시 할인가
        private LocalDate startDate; // 출발 날짜
    }
}
