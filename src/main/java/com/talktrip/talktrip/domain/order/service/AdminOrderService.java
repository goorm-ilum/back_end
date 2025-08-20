package com.talktrip.talktrip.domain.order.service;

import com.talktrip.talktrip.domain.order.dto.response.AdminOrderDetailResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderResponseDTO;
import com.talktrip.talktrip.domain.order.repository.AdminOrderRepository;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final AdminOrderRepository adminOrderRepository;

    public List<AdminOrderResponseDTO> getOrdersBySeller(CustomMemberDetails memberDetails) {
        Long sellerId = memberDetails.getId();
        return adminOrderRepository.findOrdersBySellerId(sellerId);
    }

    public Page<AdminOrderResponseDTO> getOrdersBySellerWithPagination(
            CustomMemberDetails memberDetails,
            Pageable pageable,
            String sort,
            String paymentMethod,
            String keyword,
            String orderStatus) {
        
        Long sellerId = memberDetails.getId();
        return adminOrderRepository.findOrdersBySellerIdWithPagination(
                sellerId, pageable, sort, paymentMethod, keyword, orderStatus);
    }

    public AdminOrderDetailResponseDTO getOrderDetail(String orderCode, CustomMemberDetails memberDetails) {
        Long sellerId = memberDetails.getId();

        return adminOrderRepository.findOrderDetailByOrderCodeAndSellerId(orderCode, sellerId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));
    }
}
