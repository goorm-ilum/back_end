package com.talktrip.talktrip.domain.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = {
    @Index(name = "idx_product_image_product", columnList = "product_id"),
    @Index(name = "idx_product_image_sort", columnList = "sortOrder ASC")
})
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;


}

