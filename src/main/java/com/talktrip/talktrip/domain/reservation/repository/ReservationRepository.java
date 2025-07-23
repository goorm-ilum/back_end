package com.talktrip.talktrip.domain.reservation.repository;

import com.talktrip.talktrip.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    //List<Reservation> findByUser(User user);
}
