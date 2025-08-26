package com.talktrip.talktrip.domain.review.entity;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.review.dto.request.ReviewRequest;
import com.talktrip.talktrip.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = {
    @Index(name = "idx_review_product", columnList = "product_id"),
    @Index(name = "idx_review_member", columnList = "member_id"),
    @Index(name = "idx_review_star", columnList = "reviewStar DESC"),
    @Index(name = "idx_review_product_star", columnList = "product_id, reviewStar")
})
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private String comment;

    private Double reviewStar;

    public void update(String comment, Double reviewStar) {
        this.comment = comment;
        this.reviewStar = reviewStar;
    }

    public boolean isWrittenBy(Long memberId) {
        return this.member.getId().equals(memberId);
    }

    public static Review to(Order order, Product product, Member member, ReviewRequest request) {
        return Review.builder()
                .order(order)
                .product(product)
                .member(member)
                .comment(request.comment())
                .reviewStar(request.reviewStar())
                .build();
    }

}
