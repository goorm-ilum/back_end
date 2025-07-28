package com.talktrip.talktrip.domain.reservation.dto.request;

import com.talktrip.talktrip.domain.reservation.entity.Reservation;
import lombok.Getter;

@Getter
public class ReservationRequest {
    private Long productId;
    private int peopleCount;
    private Reservation.PaymentMethod paymentMethod;
    private String paymentKey;
    private String orderId;
}
