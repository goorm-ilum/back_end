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
    
    // 새로운 상세 결제 정보
    private PaymentDetailDTO paymentDetail;
    
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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PaymentDetailDTO {
        private String paymentKey;            // 결제 키
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime approvedAt;     // 승인 일시
        private String receiptUrl;            // 영수증 URL
        private String status;                // 결제 상태
        private int totalAmount;              // 총 결제 금액
        private int vat;                      // 부가세
        private int suppliedAmount;           // 공급가액
        private boolean isPartialCancelable;  // 부분취소 가능 여부
        private CardDetailDTO cardDetail;     // 카드 결제 정보 (카드 결제인 경우만)
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CardDetailDTO {
        private String cardNumber;            // 카드번호 (마스킹 처리)
        private String issuerCode;            // 발행사 코드
        private String acquirerCode;          // 매입사 코드
        private String approveNo;             // 승인번호
        private int installmentMonths;        // 할부 개월
        private boolean isInterestFree;       // 무이자 여부
        private String cardType;              // 카드 타입 (신용/체크)
        private String ownerType;             // 소유자 타입 (개인/법인)
        private String acquireStatus;         // 승인 상태
        private int amount;                   // 카드 결제 금액
    }
}
