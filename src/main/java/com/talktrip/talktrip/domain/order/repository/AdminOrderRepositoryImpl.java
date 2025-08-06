package com.talktrip.talktrip.domain.order.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.talktrip.talktrip.domain.member.entity.QMember;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.QAdminOrderResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderDetailResponseDTO;
import com.talktrip.talktrip.domain.order.entity.QOrder;
import com.talktrip.talktrip.domain.order.entity.QOrderItem;
import com.talktrip.talktrip.domain.order.entity.QPayment;
import com.talktrip.talktrip.domain.order.entity.QCardPayment;
import com.talktrip.talktrip.domain.product.entity.QProduct;
import com.talktrip.talktrip.domain.product.entity.QProductOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AdminOrderRepositoryImpl implements AdminOrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdminOrderResponseDTO> findOrdersBySellerId(Long sellerId) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QProduct product = QProduct.product;
        QMember member = QMember.member;
        QPayment payment = QPayment.payment;

        return queryFactory
                .select(new QAdminOrderResponseDTO(
                        order.orderCode,
                        member.name,
                        product.productName,
                        order.createdAt,
                        order.totalPrice,
                        payment.method,  // Payment 엔티티에서 결제 수단 가져오기
                        order.orderStatus
                ))
                .from(order)
                .join(order.member, member)
                .join(order.orderItems, orderItem)
                .join(orderItem.product, product)
                .leftJoin(payment).on(payment.order.eq(order))  // Payment 조인 추가
                .where(product.member.Id.eq(sellerId))
                .distinct()
                .fetch();
    }

    @Override
    public Optional<AdminOrderDetailResponseDTO> findOrderDetailByOrderCodeAndSellerId(String orderCode, Long sellerId) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QProduct product = QProduct.product;
        QMember buyer = QMember.member;
        QMember seller = new QMember("seller");
        QProductOption productOption = QProductOption.productOption;
        QPayment payment = QPayment.payment;
        QCardPayment cardPayment = QCardPayment.cardPayment;

        // 주문 기본 정보 조회 (Payment 정보 포함)
        var orderInfo = queryFactory
                .select(order.orderCode, order.createdAt, order.orderDate, 
                       buyer.name, buyer.accountEmail, buyer.phoneNum,
                       order.orderStatus, payment.method, order.totalPrice,
                       payment.paymentKey, payment.approvedAt, payment.receiptUrl,
                       payment.status, payment.totalAmount, payment.vat,
                       payment.suppliedAmount, payment.isPartialCancelable,
                       cardPayment.cardNumber, cardPayment.issuerCode, cardPayment.acquirerCode,
                       cardPayment.approveNo, cardPayment.installmentMonths, cardPayment.isInterestFree,
                       cardPayment.cardType, cardPayment.ownerType, cardPayment.acquireStatus, cardPayment.amount)
                .from(order)
                .join(order.member, buyer)
                .join(order.orderItems, orderItem)
                .join(orderItem.product, product)
                .join(product.member, seller)
                .leftJoin(payment).on(payment.order.eq(order))
                .leftJoin(cardPayment).on(cardPayment.payment.eq(payment))
                .where(order.orderCode.eq(orderCode)
                        .and(seller.Id.eq(sellerId)))
                .fetchFirst();

        if (orderInfo == null) {
            return Optional.empty();
        }

        // 주문 상품 목록 조회 (옵션 정보 포함)
        List<AdminOrderDetailResponseDTO.OrderItemDetailDTO> orderItems = queryFactory
                .select(com.querydsl.core.types.Projections.constructor(AdminOrderDetailResponseDTO.OrderItemDetailDTO.class,
                        product.productName,
                        productOption.optionName,
                        orderItem.quantity,
                        productOption.price,
                        productOption.discountPrice,
                        productOption.discountPrice.multiply(orderItem.quantity),
                        productOption.startDate
                ))
                .from(orderItem)
                .join(orderItem.product, product)
                .join(orderItem.productOption, productOption)
                .join(product.member, seller)
                .where(orderItem.order.orderCode.eq(orderCode)
                        .and(seller.Id.eq(sellerId)))
                .fetch();

        // 할인 정보 계산
        int originalTotalPrice = orderItems.stream()
                .mapToInt(item -> item.getOriginalPrice() * item.getQuantity())
                .sum();
        int discountTotalPrice = orderItems.stream()
                .mapToInt(item -> item.getDiscountPrice() * item.getQuantity())
                .sum();
        int discountAmount = originalTotalPrice - discountTotalPrice;

        // PaymentDetailDTO 생성
        AdminOrderDetailResponseDTO.PaymentDetailDTO paymentDetail = null;
        if (orderInfo.get(payment.paymentKey) != null) {
            AdminOrderDetailResponseDTO.CardDetailDTO cardDetail = null;
            if (orderInfo.get(cardPayment.cardNumber) != null) {
                cardDetail = AdminOrderDetailResponseDTO.CardDetailDTO.builder()
                        .cardNumber(orderInfo.get(cardPayment.cardNumber))
                        .issuerCode(orderInfo.get(cardPayment.issuerCode))
                        .acquirerCode(orderInfo.get(cardPayment.acquirerCode))
                        .approveNo(orderInfo.get(cardPayment.approveNo))
                        .installmentMonths(orderInfo.get(cardPayment.installmentMonths) != null ? orderInfo.get(cardPayment.installmentMonths) : 0)
                        .isInterestFree(orderInfo.get(cardPayment.isInterestFree) != null ? orderInfo.get(cardPayment.isInterestFree) : false)
                        .cardType(orderInfo.get(cardPayment.cardType))
                        .ownerType(orderInfo.get(cardPayment.ownerType))
                        .acquireStatus(orderInfo.get(cardPayment.acquireStatus))
                        .amount(orderInfo.get(cardPayment.amount) != null ? orderInfo.get(cardPayment.amount) : 0)
                        .build();
            }

            paymentDetail = AdminOrderDetailResponseDTO.PaymentDetailDTO.builder()
                    .paymentKey(orderInfo.get(payment.paymentKey))
                    .approvedAt(orderInfo.get(payment.approvedAt))
                    .receiptUrl(orderInfo.get(payment.receiptUrl))
                    .status(orderInfo.get(payment.status))
                    .totalAmount(orderInfo.get(payment.totalAmount) != null ? orderInfo.get(payment.totalAmount) : 0)
                    .vat(orderInfo.get(payment.vat) != null ? orderInfo.get(payment.vat) : 0)
                    .suppliedAmount(orderInfo.get(payment.suppliedAmount) != null ? orderInfo.get(payment.suppliedAmount) : 0)
                    .isPartialCancelable(orderInfo.get(payment.isPartialCancelable) != null ? orderInfo.get(payment.isPartialCancelable) : false)
                    .cardDetail(cardDetail)
                    .build();
        }

        // Builder를 사용하여 완전한 DTO 생성
        AdminOrderDetailResponseDTO completeOrderDetail = AdminOrderDetailResponseDTO.builder()
                .orderCode(orderInfo.get(order.orderCode))
                .orderDateTime(orderInfo.get(order.createdAt))
                .orderDate(orderInfo.get(order.orderDate))
                .buyerName(orderInfo.get(buyer.name))
                .buyerEmail(orderInfo.get(buyer.accountEmail))
                .buyerPhoneNum(orderInfo.get(buyer.phoneNum))
                .orderStatus(orderInfo.get(order.orderStatus))
                .paymentMethod(orderInfo.get(payment.method))
                .originalPrice(originalTotalPrice)
                .discountAmount(discountAmount)
                .totalPrice(orderInfo.get(order.totalPrice))
                .paymentDetail(paymentDetail)
                .orderItems(orderItems)
                .build();

        return Optional.of(completeOrderDetail);
    }
}
