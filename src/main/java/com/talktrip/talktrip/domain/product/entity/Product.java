package com.talktrip.talktrip.domain.product.entity;

import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.user.entity.User;
import com.talktrip.talktrip.global.entity.BaseEntity;
import com.talktrip.talktrip.global.entity.Country;
import jakarta.persistence.*;
import lombok.*;

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

    private int price;

    private int discountPrice;

    private String regDate;

    private String endDate;

    private int leftCount;

    private String thumbnailImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User seller;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Like> likes = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<HashTag> hashtags = new ArrayList<>();
}

