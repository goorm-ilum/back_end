package com.talktrip.talktrip.domain.order.entity;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.order.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "order_date")
    private LocalDate orderDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus = OrderStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = true)
    private PaymentMethod paymentMethod;
    
    @Column(name = "total_price")
    private int totalPrice;

    @Column(name = "order_code", unique = true, nullable = false)
    private String orderCode;

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public static Order createOrder(Member member, LocalDate orderDate, PaymentMethod paymentMethod, int totalPrice) {
        Order order = new Order();
        order.member = member;
        order.createdAt = LocalDateTime.now();
        order.orderDate = orderDate;
        order.paymentMethod = paymentMethod;
        order.totalPrice = totalPrice;
        order.orderStatus = OrderStatus.PENDING;
        return order;
    }

    public void updatePaymentInfo(PaymentMethod method, OrderStatus status) {
        this.paymentMethod = method;
        this.orderStatus = status;
    }
}