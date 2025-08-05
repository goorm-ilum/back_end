package com.talktrip.talktrip.domain.order.dto.response;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.entity.OrderItem;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class OrderDetailResponseDTO {

    private final String orderId;
    private final LocalDateTime orderCreatedAt; // 주문일 (createdAt)
    private final LocalDate useDate; // 사용일 (orderDate)
    private final String paymentMethod;
    private final int totalPrice;
    private final String orderStatus;

    private final MemberInfoDTO member;
    private final List<OrderItemDTO> orderItems;

    public OrderDetailResponseDTO(Order order) {
        this.orderId = order.getOrderCode();
        this.orderCreatedAt = order.getCreatedAt(); // 주문 생성일
        this.useDate = order.getOrderDate(); // 사용일
        this.paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null;
        this.totalPrice = order.getTotalPrice();
        this.orderStatus = order.getOrderStatus().name();

        this.member = new MemberInfoDTO(order.getMember());
        this.orderItems = order.getOrderItems().stream()
                .map(OrderItemDTO::new)
                .collect(Collectors.toList());
    }

    @Getter
    public static class MemberInfoDTO {
        private final String name;
        private final String email;
        private final String phone;

        public MemberInfoDTO(Member member) {
            this.name = member.getName();
            this.email = member.getAccountEmail();
            this.phone = member.getPhoneNum();
        }
    }

    @Getter
    public static class OrderItemDTO {
        private final Long id;
        private final String productName;
        private final String productThumbnail;
        private final String optionName;
        private final int quantity;
        private final int unitPrice;
        private final int totalItemPrice;

        public OrderItemDTO(OrderItem orderItem) {
            Product product = orderItem.getProduct();
            ProductOption option = orderItem.getProductOption();

            this.id = orderItem.getId();
            this.productName = product.getProductName();
            this.productThumbnail = product.getThumbnailImageUrl();
            this.optionName = option.getOptionName();
            this.quantity = orderItem.getQuantity();
            this.unitPrice = orderItem.getPrice();
            this.totalItemPrice = unitPrice * quantity;
        }
    }

    public static OrderDetailResponseDTO from(Order order) {
        return new OrderDetailResponseDTO(order);
    }

}

