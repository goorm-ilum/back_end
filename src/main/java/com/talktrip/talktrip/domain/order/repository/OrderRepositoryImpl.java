package com.talktrip.talktrip.domain.order.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.talktrip.talktrip.domain.order.dto.response.OrderDetailWithPaymentDTO;
import com.talktrip.talktrip.domain.order.entity.QOrder;
import com.talktrip.talktrip.domain.order.entity.QPayment;
import com.talktrip.talktrip.domain.order.entity.QCardPayment;
import com.talktrip.talktrip.domain.member.entity.QMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<OrderDetailWithPaymentDTO> findOrderDetailWithPayment(Long orderId) {
        QOrder order = QOrder.order;
        QPayment payment = QPayment.payment;
        QCardPayment cardPayment = QCardPayment.cardPayment;
        QMember member = QMember.member;

        OrderDetailWithPaymentDTO result = queryFactory
                .select(com.querydsl.core.types.Projections.constructor(OrderDetailWithPaymentDTO.class,
                        order.orderCode,
                        order.createdAt,
                        order.orderDate,
                        order.totalPrice,
                        order.orderStatus.stringValue(),
                        member.Id,
                        member.name,
                        member.accountEmail,
                        member.phoneNum,
                        payment.method.stringValue(),
                        payment.paymentKey,
                        payment.approvedAt,
                        payment.receiptUrl,
                        payment.status,
                        payment.totalAmount,
                        payment.vat,
                        payment.suppliedAmount,
                        payment.isPartialCancelable,
                        cardPayment.cardNumber,
                        cardPayment.issuerCode,
                        cardPayment.acquirerCode,
                        cardPayment.approveNo,
                        cardPayment.installmentMonths,
                        cardPayment.isInterestFree,
                        cardPayment.cardType,
                        cardPayment.ownerType,
                        cardPayment.acquireStatus,
                        cardPayment.amount
                ))
                .from(order)
                .leftJoin(order.member, member)
                .leftJoin(payment).on(payment.order.eq(order))
                .leftJoin(cardPayment).on(cardPayment.payment.eq(payment))
                .where(order.id.eq(orderId))
                .fetchFirst();

        return Optional.ofNullable(result);
    }
} 