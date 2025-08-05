package com.talktrip.talktrip.domain.order.repository;

import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByMemberIdAndOrderStatus(Long memberId, OrderStatus orderStatus);

}
