package com.talktrip.talktrip.domain.like.entity;

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
@Table(name = "likes", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "member_id"})
    },
    indexes = {
        @Index(name = "idx_like_member", columnList = "member_id"),
        @Index(name = "idx_like_product", columnList = "product_id"),
        @Index(name = "idx_like_member_product", columnList = "member_id, product_id")
    }
)
public class Like extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;


}
