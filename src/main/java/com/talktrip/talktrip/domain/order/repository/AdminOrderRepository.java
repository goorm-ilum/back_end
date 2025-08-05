package com.talktrip.talktrip.domain.order.repository;

import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderDetailResponseDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminOrderRepository extends JpaRepository<Order, Long>, AdminOrderRepositoryCustom {

    Optional<AdminOrderDetailResponseDTO> findOrderDetailByOrderCodeAndSellerId(String orderCode, Long sellerId);
}
