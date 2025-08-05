package com.talktrip.talktrip.domain.order.dto.response;

import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.entity.OrderItem;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderHistoryResponseDTO {
    private Long orderId;
    private String productName;
    private String paymentMethod;
    private int totalPrice;
    private LocalDateTime createdAt;

    public OrderHistoryResponseDTO(Long orderId, String productName, String paymentMethod, int totalPrice, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.productName = productName;
        this.paymentMethod = paymentMethod;
        this.totalPrice = totalPrice;
        this.createdAt = createdAt;
    }

    public static OrderHistoryResponseDTO fromEntity(Order order) {
        OrderItem firstItem = order.getOrderItems().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("주문에 상품이 없습니다."));

        String productName = firstItem.getProduct().getProductName();

        return new OrderHistoryResponseDTO(
                order.getId(),
                productName,
                order.getPaymentMethod().name(),
                order.getTotalPrice(),
                order.getCreatedAt()
        );
    }
}