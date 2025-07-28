package com.talktrip.talktrip.domain.reservation.dto.response;

import com.talktrip.talktrip.domain.reservation.entity.Reservation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationResponse {
    private Long reservationId;
    private String productName;
    private int totalAmount;
    private int peopleCount;
    private Reservation.PaymentMethod paymentMethod;
    private String orderId;
    private LocalDateTime reservationDate;

    public static ReservationResponse from(Reservation r) {
        return ReservationResponse.builder()
                .reservationId(r.getId())
                .productName(r.getProduct().getName())
                .totalAmount(r.getTotalAmount())
                .peopleCount(r.getPeopleCount())
                .paymentMethod(r.getPaymentMethod())
                .orderId(r.getOrderId())
                .reservationDate(r.getReservationDate())
                .build();
    }
}
