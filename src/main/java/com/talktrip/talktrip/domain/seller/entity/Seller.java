package com.talktrip.talktrip.domain.seller.entity;

import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Seller extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String nickname;

    private String role;

    @Builder.Default
    @OneToMany(mappedBy = "seller")
    private List<Product> products = new ArrayList<>();
}
