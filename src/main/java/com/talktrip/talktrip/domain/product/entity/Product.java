package com.talktrip.talktrip.domain.product.entity;

import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.global.entity.BaseEntity;
import com.talktrip.talktrip.global.entity.Country;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE product SET deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted = false") // 기본 조회에서 삭제 제외
public class Product extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String productName;

    @Column(length = 1000, nullable = false)
    private String description;

    private String thumbnailImageUrl;
    private String thumbnailImageHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HashTag> hashtags = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> productOptions = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    public void markDeleted() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
    }

    public void updateThumbnailImage(String url, String hash) {
        this.thumbnailImageUrl = url;
        this.thumbnailImageHash = hash;
    }

    public void updateBasicInfo(String productName, String description, Country country) {
        this.productName = productName;
        this.description = description;
        this.country = country;
    }

    public ProductOption getMinPriceOption() {
        return productOptions.stream()
                .filter(option -> !option.getStartDate().isBefore(LocalDate.now()))
                .min(Comparator.comparingInt(ProductOption::getDiscountPrice))
                .orElse(null);
    }

    public int getTotalStock() {
        return productOptions.stream().mapToInt(ProductOption::getStock).sum();
    }
}
