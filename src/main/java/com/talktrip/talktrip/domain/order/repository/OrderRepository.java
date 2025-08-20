package com.talktrip.talktrip.domain.order.repository;

import com.talktrip.talktrip.domain.order.dto.response.OrderDetailWithPaymentDTO;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByMemberIdAndOrderStatus(Long memberId, OrderStatus orderStatus);

    // 페이지네이션을 지원하는 메서드 추가
    Page<Order> findByMemberIdAndOrderStatus(Long memberId, OrderStatus orderStatus, Pageable pageable);

    // 여러 주문 상태를 조회하는 메서드 추가
    Page<Order> findByMemberIdAndOrderStatusIn(Long memberId, List<OrderStatus> orderStatuses, Pageable pageable);

    // QueryDSL을 사용한 복합 조회 메서드
    Optional<OrderDetailWithPaymentDTO> findOrderDetailWithPayment(Long orderId);
}
