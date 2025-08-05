package com.talktrip.talktrip.domain.order.repository;

import com.talktrip.talktrip.domain.order.dto.response.AdminOrderResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderDetailResponseDTO;

import java.util.List;
import java.util.Optional;

public interface AdminOrderRepositoryCustom {
    List<AdminOrderResponseDTO> findOrdersBySellerId(Long sellerId);
    
    Optional<AdminOrderDetailResponseDTO> findOrderDetailByOrderCodeAndSellerId(String orderCode, Long sellerId);
}
