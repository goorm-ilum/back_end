package com.talktrip.talktrip.domain.order.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.querydsl.core.annotations.QueryProjection;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.order.enums.PaymentMethod;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminOrderResponseDTO {
    private String orderCode;              // 주문번호
    private String memberName;             // 주문자 이름
    private String productName;            // 상품명

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;       // 주문일

    private int totalPrice;                // 금액
    private PaymentMethod paymentMethod;   // 결제수단
    private OrderStatus orderStatus;       // 주문상태

    @QueryProjection
    public AdminOrderResponseDTO(String orderCode, String memberName, String productName,
                                 LocalDateTime createdAt, int totalPrice,
                                 PaymentMethod paymentMethod, OrderStatus orderStatus) {
        this.orderCode = orderCode;
        this.memberName = memberName;
        this.productName = productName;
        this.createdAt = createdAt;
        this.totalPrice = totalPrice;
        this.paymentMethod = paymentMethod;
        this.orderStatus = orderStatus;
    }
}
