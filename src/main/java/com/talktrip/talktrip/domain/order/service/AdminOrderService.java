package com.talktrip.talktrip.domain.order.service;

import jakarta.persistence.EntityNotFoundException;
import com.talktrip.talktrip.domain.order.repository.AdminOrderRepository;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderDetailResponseDTO;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import lombok.RequiredArgsConstructor;
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

    public AdminOrderDetailResponseDTO getOrderDetail(String orderCode, CustomMemberDetails memberDetails) {
        Long sellerId = memberDetails.getId();

        return adminOrderRepository.findOrderDetailByOrderCodeAndSellerId(orderCode, sellerId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));
    }
}
