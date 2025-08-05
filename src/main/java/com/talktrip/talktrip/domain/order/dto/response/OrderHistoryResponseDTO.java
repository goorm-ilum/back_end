package com.talktrip.talktrip.domain.order.dto.response;

import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.entity.OrderItem;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderHistoryResponseDTO {
    private Long orderId;
    private Long productId;
    private String productName;
    private String paymentMethod;
    private int totalPrice;
    private LocalDateTime createdAt;

    public OrderHistoryResponseDTO(Long orderId, Long productId, String productName, String paymentMethod, int totalPrice, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.paymentMethod = paymentMethod;
        this.totalPrice = totalPrice;
        this.createdAt = createdAt;
    }

    public static OrderHistoryResponseDTO fromEntity(Order order) {
        OrderItem firstItem = order.getOrderItems().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("주문에 상품이 없습니다."));

        String productName = firstItem.getProduct().getProductName();
        Long productId = firstItem.getProduct().getId();

        return new OrderHistoryResponseDTO(
                order.getId(),
                productId,
                productName,
                order.getPaymentMethod().name(),
                order.getTotalPrice(),
                order.getCreatedAt()
        );
    }
}