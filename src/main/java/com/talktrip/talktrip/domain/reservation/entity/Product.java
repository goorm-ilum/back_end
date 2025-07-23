package com.talktrip.talktrip.domain.reservation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상품 이름 (ex. 인천→도쿄 왕복 항공권, 스위스 3박 4일 패키지)
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 상품 가격 (1인당)
    @Column(nullable = false)
    private int price;

    // 남은 수량 (예약 가능한 인원 수)
    private int stock;

    // 출발일시 (여행 시작일, 항공권 출발일 포함)
    private LocalDateTime departureDate;

    // 도착일시 (여행 종료일, 항공권 도착일 포함)
    private LocalDateTime arrivalDate;

    @Enumerated(EnumType.STRING)
    private ProductType type;

    public enum ProductType {
        FLIGHT, TOUR
    }
}