package com.talktrip.talktrip.domain.order.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.talktrip.talktrip.domain.member.entity.QMember;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.QAdminOrderResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderDetailResponseDTO;
import com.talktrip.talktrip.domain.order.entity.QOrder;
import com.talktrip.talktrip.domain.order.entity.QOrderItem;
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

        return queryFactory
                .select(new QAdminOrderResponseDTO(
                        order.orderCode,
                        member.name,
                        product.productName,
                        order.createdAt,
                        order.totalPrice,
                        order.paymentMethod,
                        order.orderStatus
                ))
                .from(order)
                .join(order.member, member)
                .join(order.orderItems, orderItem)
                .join(orderItem.product, product)
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

        // 주문 기본 정보 조회
        var orderInfo = queryFactory
                .select(order.orderCode, order.createdAt, order.orderDate, 
                       buyer.name, buyer.accountEmail, buyer.phoneNum,
                       order.orderStatus, order.paymentMethod, order.totalPrice)
                .from(order)
                .join(order.member, buyer)
                .join(order.orderItems, orderItem)
                .join(orderItem.product, product)
                .join(product.member, seller)
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

        // Builder를 사용하여 완전한 DTO 생성
        AdminOrderDetailResponseDTO completeOrderDetail = AdminOrderDetailResponseDTO.builder()
                .orderCode(orderInfo.get(order.orderCode))
                .orderDateTime(orderInfo.get(order.createdAt))
                .orderDate(orderInfo.get(order.orderDate))
                .buyerName(orderInfo.get(buyer.name))
                .buyerEmail(orderInfo.get(buyer.accountEmail))
                .buyerPhoneNum(orderInfo.get(buyer.phoneNum))
                .orderStatus(orderInfo.get(order.orderStatus))
                .paymentMethod(orderInfo.get(order.paymentMethod))
                .originalPrice(originalTotalPrice)
                .discountAmount(discountAmount)
                .totalPrice(orderInfo.get(order.totalPrice))
                .orderItems(orderItems)
                .build();

        return Optional.of(completeOrderDetail);
    }
}
