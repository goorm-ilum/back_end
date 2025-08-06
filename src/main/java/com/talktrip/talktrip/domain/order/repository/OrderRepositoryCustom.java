package com.talktrip.talktrip.domain.order.repository;

import com.talktrip.talktrip.domain.order.dto.response.OrderDetailWithPaymentDTO;

import java.util.Optional;

public interface OrderRepositoryCustom {
    Optional<OrderDetailWithPaymentDTO> findOrderDetailWithPayment(Long orderId);
} 