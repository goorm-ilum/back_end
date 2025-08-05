package com.talktrip.talktrip.domain.order.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.order.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AdminOrderDetailResponseDTO {
    private String orderCode;                 // 주문 고유 코드
    
    // 고객 정보
    private String buyerName;                 // 주문자 이름
    private String buyerEmail;                // 주문자 이메일
    private String buyerPhoneNum;             // 주문자 전화번호
    
    // 결제 정보
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime orderDateTime;      // 주문 일시
    private LocalDate orderDate;              // 상품 이용일
    private PaymentMethod paymentMethod;      // 결제 수단
    private int originalPrice;                // 할인 전 총 금액
    private int discountAmount;               // 할인 금액
    private int totalPrice;                   // 총 결제 금액
    private OrderStatus orderStatus;          // 주문 상태
    
    // 주문 상품 목록
    private List<OrderItemDetailDTO> orderItems;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OrderItemDetailDTO {
        private String productName;           // 상품명
        private String optionName;            // 옵션명
        private int quantity;                 // 수량
        private int originalPrice;            // 할인 전 가격
        private int discountPrice;            // 할인 후 가격
        private int totalPrice;               // 해당 상품 총 가격 (할인 후 * 수량)
        private LocalDate useDate;            // 상품 이용일
    }
}
