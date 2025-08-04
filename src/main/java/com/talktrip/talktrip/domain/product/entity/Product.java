package com.talktrip.talktrip.domain.product.entity;

import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.global.entity.BaseEntity;
import com.talktrip.talktrip.global.entity.Country;
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
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String productName;

    @Column(length = 50, nullable = false)
    private String description;

    private String thumbnailImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HashTag> hashtags = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductStock> productStocks = new ArrayList<>();

    public void updateBasicInfo(String productName, String description, String thumbnailImageUrl, Country country) {
        this.productName = productName;
        this.description = description;
        this.thumbnailImageUrl = thumbnailImageUrl;
        this.country = country;
    }

    public ProductStock getMinPriceStock() {
        return productStocks.stream()
                .min((s1, s2) -> Integer.compare(s1.getPrice(), s2.getPrice()))
                .orElse(null);
    }
}