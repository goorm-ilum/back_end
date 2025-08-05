package com.talktrip.talktrip.domain.order.dto.response;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.talktrip.talktrip.domain.order.dto.response.QAdminOrderResponseDTO is a Querydsl Projection type for AdminOrderResponseDTO
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QAdminOrderResponseDTO extends ConstructorExpression<AdminOrderResponseDTO> {

    private static final long serialVersionUID = -923423298L;

    public QAdminOrderResponseDTO(com.querydsl.core.types.Expression<String> orderCode, com.querydsl.core.types.Expression<String> memberName, com.querydsl.core.types.Expression<String> productName, com.querydsl.core.types.Expression<java.time.LocalDateTime> createdAt, com.querydsl.core.types.Expression<Integer> totalPrice, com.querydsl.core.types.Expression<com.talktrip.talktrip.domain.order.enums.PaymentMethod> paymentMethod, com.querydsl.core.types.Expression<com.talktrip.talktrip.domain.order.enums.OrderStatus> orderStatus) {
        super(AdminOrderResponseDTO.class, new Class<?>[]{String.class, String.class, String.class, java.time.LocalDateTime.class, int.class, com.talktrip.talktrip.domain.order.enums.PaymentMethod.class, com.talktrip.talktrip.domain.order.enums.OrderStatus.class}, orderCode, memberName, productName, createdAt, totalPrice, paymentMethod, orderStatus);
    }

}

