package com.talktrip.talktrip.domain.order.repository;

import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.order.dto.response.OrderDetailWithPaymentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByMemberIdAndOrderStatus(Long memberId, OrderStatus orderStatus);

    // 페이지네이션을 지원하는 메서드 추가
    Page<Order> findByMemberIdAndOrderStatus(Long memberId, OrderStatus orderStatus, Pageable pageable);

    // QueryDSL을 사용한 복합 조회 메서드
    Optional<OrderDetailWithPaymentDTO> findOrderDetailWithPayment(Long orderId);
}
