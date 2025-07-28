package com.talktrip.talktrip.domain.reservation.entity;

import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@ManyToOne(fetch = FetchType.LAZY)
    //private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    private int peopleCount;
    private int totalAmount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String paymentKey; // PG 식별자
    private String orderId;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private LocalDateTime reservationDate;

    public enum PaymentMethod {
        CREDIT_CARD, ACCOUNT_TRANSFER, KAKAO_PAY, TOSS
    }

    public enum ReservationStatus {
        COMPLETED, CANCELLED
    }
}
