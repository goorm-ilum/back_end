package com.talktrip.talktrip.domain.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reservation", description = "예약 및 결제 API")
@RestController
@RequestMapping("/api")
public class ReservationController {

    @Operation(summary = "예약 및 결제")
    @PostMapping("/reservation")
    public void reserve() {}

    @Operation(summary = "예약 현황 조회")
    @GetMapping("/me/reservations")
    public void getMyReservations() {}

    @Operation(summary = "예약 상세 조회")
    @GetMapping("/me/reservations/{reservationId}")
    public void getReservationDetail(@PathVariable Long reservationId) {}

}
