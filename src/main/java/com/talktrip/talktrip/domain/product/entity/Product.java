package com.talktrip.talktrip.domain.product.entity;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.global.entity.BaseEntity;
import com.talktrip.talktrip.global.entity.Country;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE product SET deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
@Table(indexes = {
    @Index(name = "idx_product_search", columnList = "deleted, country_id, updatedAt"),
    @Index(name = "idx_product_name_search", columnList = "productName"),
    @Index(name = "idx_product_updated_at", columnList = "updatedAt DESC"),
    @Index(name = "idx_product_seller", columnList = "seller_id, deleted")
})
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

    private static final int DAYS_TO_ADD = 1;

    private Stream<ProductOption> getFutureOptions() {
        LocalDate tomorrow = LocalDate.now().plusDays(DAYS_TO_ADD);
        return productOptions.stream()
                .filter(option -> !option.getStartDate().isBefore(tomorrow));
    }

    public ProductOption getMinPriceOption() {
        return getFutureOptions()
                .min(Comparator.comparingInt(ProductOption::getDiscountPrice))
                .orElse(null);
    }

    public String getCountryName() {
        return country != null ? country.getName() : "";
    }

    public float getAverageReviewStar() {
        return (float) reviews.stream()
                .mapToDouble(Review::getReviewStar)
                .average()
                .orElse(0.0);
    }
}
