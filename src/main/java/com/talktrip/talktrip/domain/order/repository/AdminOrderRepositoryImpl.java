package com.talktrip.talktrip.domain.order.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.talktrip.talktrip.domain.member.entity.QMember;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderDetailResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.QAdminOrderResponseDTO;
import com.talktrip.talktrip.domain.order.entity.QCardPayment;
import com.talktrip.talktrip.domain.order.entity.QOrder;
import com.talktrip.talktrip.domain.order.entity.QOrderItem;
import com.talktrip.talktrip.domain.order.entity.QPayment;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.order.enums.PaymentMethod;
import com.talktrip.talktrip.domain.product.entity.QProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class AdminOrderRepositoryImpl implements AdminOrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdminOrderResponseDTO> findOrdersBySellerId(Long sellerId) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QMember member = QMember.member;
        QPayment payment = QPayment.payment;
        QProduct product = QProduct.product;
        return queryFactory
                .select(new QAdminOrderResponseDTO(
                        order.orderCode,
                        member.name,
                        orderItem.productName, // 스냅샷 필드 사용
                        order.createdAt,
                        order.totalPrice,
                        payment.method,
                        order.orderStatus
                ))
                .from(order)
                .join(order.member, member)
                .join(order.orderItems, orderItem)
                .leftJoin(payment).on(payment.order.eq(order))
                .where(orderItem.productId.in(
                        JPAExpressions
                                .select(product.id)
                                .from(product)
                                .where(product.member.Id.eq(sellerId))
                )) // 서브쿼리로 판매자의 상품 ID들만 필터링
                .distinct()
                .fetch();
    }

    @Override
    public Page<AdminOrderResponseDTO> findOrdersBySellerIdWithPagination(
            Long sellerId,
            Pageable pageable,
            String sort,
            String paymentMethod,
            String keyword,
            String orderStatus) {

        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QMember member = QMember.member;
        QPayment payment = QPayment.payment;
        QProduct product = QProduct.product;

        // 기본 조건: 판매자의 상품 ID들만 필터링
        BooleanBuilder whereClause = new BooleanBuilder();
        whereClause.and(orderItem.productId.in(
                JPAExpressions
                        .select(product.id)
                        .from(product)
                        .where(product.member.Id.eq(sellerId))
        ));

        // 결제수단 필터
        if (paymentMethod != null && !paymentMethod.trim().isEmpty()) {
            try {
                PaymentMethod method = PaymentMethod.valueOf(paymentMethod.toUpperCase());
                whereClause.and(payment.method.eq(method));
            } catch (IllegalArgumentException e) {
                // 잘못된 결제수단 값은 무시
            }
        }

        // 검색어 필터 (상품명 - 스냅샷 필드 사용)
        if (keyword != null && !keyword.trim().isEmpty()) {
            whereClause.and(orderItem.productName.containsIgnoreCase(keyword.trim()));
        }

        // 주문상태 필터
        if (orderStatus != null && !orderStatus.trim().isEmpty()) {
            try {
                OrderStatus status = OrderStatus.valueOf(orderStatus.toUpperCase());
                whereClause.and(order.orderStatus.eq(status));
            } catch (IllegalArgumentException e) {
                // 잘못된 주문상태 값은 무시
            }
        }

        // 정렬 조건
        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(order, sort);

        // 전체 개수 조회
        long total = queryFactory
                .select(order.count().coalesce(0L))
                .from(order)
                .join(order.member, member)
                .join(order.orderItems, orderItem)
                .leftJoin(payment).on(payment.order.eq(order))
                .where(whereClause)
                .distinct()
                .fetchOne();

        // 페이지 데이터 조회
        List<AdminOrderResponseDTO> content = queryFactory
                .select(new QAdminOrderResponseDTO(
                        order.orderCode,
                        member.name,
                        orderItem.productName,
                        order.createdAt,
                        order.totalPrice,
                        payment.method,
                        order.orderStatus
                ))
                .from(order)
                .join(order.member, member)
                .join(order.orderItems, orderItem)
                .leftJoin(payment).on(payment.order.eq(order))
                .where(whereClause)
                .orderBy(orderSpecifier)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .distinct()
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    private OrderSpecifier<?> getOrderSpecifier(QOrder order, String sort) {
        switch (sort.toLowerCase()) {
            case "amount":
                return order.totalPrice.desc();
            case "id":
                return order.orderCode.asc();
            case "date":
            default:
                return order.createdAt.desc();
        }
    }

    @Override
    public Optional<AdminOrderDetailResponseDTO> findOrderDetailByOrderCodeAndSellerId(String orderCode, Long sellerId) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QMember buyer = QMember.member;
        QPayment payment = QPayment.payment;
        QCardPayment cardPayment = QCardPayment.cardPayment;
        QProduct product = QProduct.product;

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
                .leftJoin(payment).on(payment.order.eq(order))
                .leftJoin(cardPayment).on(cardPayment.payment.eq(payment))
                .where(order.orderCode.eq(orderCode)
                        .and(orderItem.productId.in(
                                JPAExpressions
                                        .select(product.id)
                                        .from(product)
                                        .where(product.member.Id.eq(sellerId))
                        )))
                .fetchFirst();

        if (orderInfo == null) {
            return Optional.empty();
        }

        // 주문 상품 목록 조회 (스냅샷 데이터 사용)
        List<AdminOrderDetailResponseDTO.OrderItemDetailDTO> orderItems = queryFactory
                .select(com.querydsl.core.types.Projections.constructor(AdminOrderDetailResponseDTO.OrderItemDetailDTO.class,
                        orderItem.productName,
                        orderItem.optionName,
                        orderItem.quantity,
                        orderItem.optionPrice,
                        orderItem.optionDiscountPrice,
                        orderItem.optionDiscountPrice.multiply(orderItem.quantity),
                        orderItem.startDate
                ))
                .from(orderItem)
                .where(orderItem.order.orderCode.eq(orderCode)
                        .and(orderItem.productId.in(
                                JPAExpressions
                                        .select(product.id)
                                        .from(product)
                                        .where(product.member.Id.eq(sellerId))
                        )))
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