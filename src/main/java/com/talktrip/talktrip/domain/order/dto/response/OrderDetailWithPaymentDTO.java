package com.talktrip.talktrip.domain.order.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class OrderDetailWithPaymentDTO {
    // Order 정보
    private final String orderId;
    private final LocalDateTime orderCreatedAt;
    private final LocalDate useDate;
    private final int totalPrice;
    private final String orderStatus;
    
    // Member 정보
    private final Long memberId;
    private final String memberName;
    private final String memberEmail;
    private final String memberPhone;
    
    // Payment 정보
    private final String paymentMethod;
    private final String paymentKey;
    private final LocalDateTime approvedAt;
    private final String receiptUrl;
    private final String paymentStatus;
    private final int paymentTotalAmount;
    private final int vat;
    private final int suppliedAmount;
    private final boolean isPartialCancelable;
    
    // CardPayment 정보 (카드 결제인 경우만)
    private final String cardNumber;
    private final String issuerCode;
    private final String acquirerCode;
    private final String approveNo;
    private final Integer installmentMonths;
    private final Boolean isInterestFree;
    private final String cardType;
    private final String ownerType;
    private final String acquireStatus;
    private final Integer cardAmount;

    @QueryProjection
    public OrderDetailWithPaymentDTO(
            // Order 정보
            String orderId, LocalDateTime orderCreatedAt, LocalDate useDate, 
            int totalPrice, String orderStatus,
            // Member 정보
            Long memberId, String memberName, String memberEmail, String memberPhone,
            // Payment 정보
            String paymentMethod, String paymentKey, LocalDateTime approvedAt,
            String receiptUrl, String paymentStatus, int paymentTotalAmount,
            int vat, int suppliedAmount, boolean isPartialCancelable,
            // CardPayment 정보
            String cardNumber, String issuerCode, String acquirerCode, String approveNo,
            Integer installmentMonths, Boolean isInterestFree, String cardType,
            String ownerType, String acquireStatus, Integer cardAmount) {
        
        this.orderId = orderId;
        this.orderCreatedAt = orderCreatedAt;
        this.useDate = useDate;
        this.totalPrice = totalPrice;
        this.orderStatus = orderStatus;
        
        this.memberId = memberId;
        this.memberName = memberName;
        this.memberEmail = memberEmail;
        this.memberPhone = memberPhone;
        
        this.paymentMethod = paymentMethod;
        this.paymentKey = paymentKey;
        this.approvedAt = approvedAt;
        this.receiptUrl = receiptUrl;
        this.paymentStatus = paymentStatus;
        this.paymentTotalAmount = paymentTotalAmount;
        this.vat = vat;
        this.suppliedAmount = suppliedAmount;
        this.isPartialCancelable = isPartialCancelable;
        
        this.cardNumber = cardNumber;
        this.issuerCode = issuerCode;
        this.acquirerCode = acquirerCode;
        this.approveNo = approveNo;
        this.installmentMonths = installmentMonths;
        this.isInterestFree = isInterestFree;
        this.cardType = cardType;
        this.ownerType = ownerType;
        this.acquireStatus = acquireStatus;
        this.cardAmount = cardAmount;
    }
} 