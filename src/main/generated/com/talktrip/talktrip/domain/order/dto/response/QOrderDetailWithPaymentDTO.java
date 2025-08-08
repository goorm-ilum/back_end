package com.talktrip.talktrip.domain.order.dto.response;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.talktrip.talktrip.domain.order.dto.response.QOrderDetailWithPaymentDTO is a Querydsl Projection type for OrderDetailWithPaymentDTO
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QOrderDetailWithPaymentDTO extends ConstructorExpression<OrderDetailWithPaymentDTO> {

    private static final long serialVersionUID = 844628381L;

    public QOrderDetailWithPaymentDTO(com.querydsl.core.types.Expression<String> orderId, com.querydsl.core.types.Expression<java.time.LocalDateTime> orderCreatedAt, com.querydsl.core.types.Expression<java.time.LocalDate> useDate, com.querydsl.core.types.Expression<Integer> totalPrice, com.querydsl.core.types.Expression<String> orderStatus, com.querydsl.core.types.Expression<Long> memberId, com.querydsl.core.types.Expression<String> memberName, com.querydsl.core.types.Expression<String> memberEmail, com.querydsl.core.types.Expression<String> memberPhone, com.querydsl.core.types.Expression<String> paymentMethod, com.querydsl.core.types.Expression<String> paymentKey, com.querydsl.core.types.Expression<java.time.LocalDateTime> approvedAt, com.querydsl.core.types.Expression<String> receiptUrl, com.querydsl.core.types.Expression<String> paymentStatus, com.querydsl.core.types.Expression<Integer> paymentTotalAmount, com.querydsl.core.types.Expression<Integer> vat, com.querydsl.core.types.Expression<Integer> suppliedAmount, com.querydsl.core.types.Expression<Boolean> isPartialCancelable, com.querydsl.core.types.Expression<String> cardNumber, com.querydsl.core.types.Expression<String> issuerCode, com.querydsl.core.types.Expression<String> acquirerCode, com.querydsl.core.types.Expression<String> approveNo, com.querydsl.core.types.Expression<Integer> installmentMonths, com.querydsl.core.types.Expression<Boolean> isInterestFree, com.querydsl.core.types.Expression<String> cardType, com.querydsl.core.types.Expression<String> ownerType, com.querydsl.core.types.Expression<String> acquireStatus, com.querydsl.core.types.Expression<Integer> cardAmount) {
        super(OrderDetailWithPaymentDTO.class, new Class<?>[]{String.class, java.time.LocalDateTime.class, java.time.LocalDate.class, int.class, String.class, long.class, String.class, String.class, String.class, String.class, String.class, java.time.LocalDateTime.class, String.class, String.class, int.class, int.class, int.class, boolean.class, String.class, String.class, String.class, String.class, int.class, boolean.class, String.class, String.class, String.class, int.class}, orderId, orderCreatedAt, useDate, totalPrice, orderStatus, memberId, memberName, memberEmail, memberPhone, paymentMethod, paymentKey, approvedAt, receiptUrl, paymentStatus, paymentTotalAmount, vat, suppliedAmount, isPartialCancelable, cardNumber, issuerCode, acquirerCode, approveNo, installmentMonths, isInterestFree, cardType, ownerType, acquireStatus, cardAmount);
    }

}

