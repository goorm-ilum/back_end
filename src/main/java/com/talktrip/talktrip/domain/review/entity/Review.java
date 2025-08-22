package com.talktrip.talktrip.domain.review.entity;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.product.entity.Product;
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
    // 상품별 리뷰 조회 최적화
    @Index(name = "idx_review_product", columnList = "product_id"),
    // 회원별 리뷰 조회 최적화
    @Index(name = "idx_review_member", columnList = "member_id"),
    // 별점 정렬 최적화
    @Index(name = "idx_review_star", columnList = "reviewStar DESC"),
    // 복합 인덱스: 상품별 평균 별점 계산 최적화
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

    private float reviewStar;

    public void update(String comment, float reviewStar) {
        this.comment = comment;
        this.reviewStar = reviewStar;
    }

}
