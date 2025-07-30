package com.talktrip.talktrip.domain.review.entity;

import com.talktrip.talktrip.domain.member.entity.Member;
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

    private String comment;

    private float reviewStar;

    public void update(String comment, float reviewStar) {
        this.comment = comment;
        this.reviewStar = reviewStar;
    }

}
