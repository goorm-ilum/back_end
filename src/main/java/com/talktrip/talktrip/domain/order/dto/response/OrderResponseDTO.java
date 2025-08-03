package com.talktrip.talktrip.domain.order.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderResponseDTO {

    private String orderId;       // 주문 고유 ID (예: UUID)
    private String orderName;     // 주문명 (예: "상품명 외 2건")
    private int amount;           // 총 결제 금액
    private String customerEmail; // 고객 이메일

    public OrderResponseDTO() {}

    public OrderResponseDTO(String orderId, String orderName, int amount, String customerEmail) {
        this.orderId = orderId;
        this.orderName = orderName;
        this.amount = amount;
        this.customerEmail = customerEmail;
    }
}
