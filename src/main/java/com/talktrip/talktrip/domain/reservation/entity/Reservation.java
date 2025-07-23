package com.talktrip.talktrip.domain.reservation.entity;

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
    //@JoinColumn(name = "user_id", nullable = false)
    //private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private int peopleCount;

    private LocalDateTime reservationDate;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    public enum ReservationStatus {
        RESERVED, CANCELLED, COMPLETED
    }
}
