package com.talktrip.talktrip.domain.order.repository;

import com.talktrip.talktrip.domain.order.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByPaymentKey(String paymentKey);
    
    Optional<Payment> findByOrderId(Long orderId);
} 