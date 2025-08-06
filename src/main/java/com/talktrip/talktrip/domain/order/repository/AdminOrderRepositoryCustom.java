package com.talktrip.talktrip.domain.order.repository;

import com.talktrip.talktrip.domain.order.dto.response.AdminOrderResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderDetailResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AdminOrderRepositoryCustom {
    List<AdminOrderResponseDTO> findOrdersBySellerId(Long sellerId);
    
    Page<AdminOrderResponseDTO> findOrdersBySellerIdWithPagination(
            Long sellerId, 
            Pageable pageable, 
            String sort, 
            String paymentMethod, 
            String keyword, 
            String orderStatus);
    
    Optional<AdminOrderDetailResponseDTO> findOrderDetailByOrderCodeAndSellerId(String orderCode, Long sellerId);
}
