package com.talktrip.talktrip.domain.order.dto.request;

import lombok.Getter;
import lombok.Setter;

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
        private String optionName; // 옵션 이름
        private int quantity;      // 선택 수량
    }
}
