package com.talktrip.talktrip.domain.order.repository;

import com.talktrip.talktrip.domain.order.entity.CardPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardPaymentRepository extends JpaRepository<CardPayment, Long> {

} 