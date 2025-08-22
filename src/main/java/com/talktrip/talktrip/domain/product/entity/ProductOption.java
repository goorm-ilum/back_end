package com.talktrip.talktrip.domain.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(indexes = {
    @Index(name = "idx_product_option_stock", columnList = "product_id, startDate, stock"),
    @Index(name = "idx_product_option_price", columnList = "discountPrice ASC"),
    @Index(name = "idx_product_option_date", columnList = "startDate ASC")
})
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate startDate;

    private String optionName;

    private int stock;

    private int price;

    private int discountPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public void setStock(int stock) {
        this.stock = stock;
    }

    public void addStock(int quantity) {
        this.stock += quantity;
    }
}

