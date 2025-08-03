package com.talktrip.talktrip.domain.order.entity;

import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id")
    private ProductOption productOption;

    private int quantity;

    private int price;

    public void setOrder(Order order) {
        this.order = order;
    }

    // 정적 팩토리 메서드
    public static OrderItem createOrderItem(Product product, ProductOption productOption, int quantity, int price) {
        OrderItem orderItem = new OrderItem();
        orderItem.product = product;
        orderItem.productOption = productOption;
        orderItem.quantity = quantity;
        orderItem.price = price;
        return orderItem;
    }
}