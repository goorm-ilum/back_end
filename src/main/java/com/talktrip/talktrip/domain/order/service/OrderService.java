package com.talktrip.talktrip.domain.order.service;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.order.dto.request.OrderRequestDTO;
import com.talktrip.talktrip.domain.order.dto.response.OrderResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.OrderHistoryResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.OrderDetailResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderDetailResponseDTO;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.entity.OrderItem;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.order.repository.OrderRepository;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.product.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;

    public OrderResponseDTO createOrder(Long productId, OrderRequestDTO orderRequest, Long memberId) {

        // 1. 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        // 2. 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        // 3. 주문 항목 생성
        List<OrderItem> orderItems = orderRequest.getOptions().stream()
                .map(optReq -> {
                    ProductOption productOption = productOptionRepository.findByProductIdAndOptionName(
                                    productId, optReq.getOptionName())
                            .orElseThrow(() -> new IllegalArgumentException("옵션을 찾을 수 없습니다: " + optReq.getOptionName()));

                    int itemTotalPrice = productOption.getPrice() * optReq.getQuantity();

                    return OrderItem.createOrderItem(
                            product,
                            productOption,
                            optReq.getQuantity(),
                            itemTotalPrice
                    );
                })
                .collect(Collectors.toList());

        // 4. 주문명 생성
        int totalQuantity = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();
        String orderName = (totalQuantity == 1)
                ? product.getProductName() + " - 단일 옵션"
                : product.getProductName() + " 외 " + (totalQuantity - 1) + "건";

        // 5. 주문 생성
        Order order = Order.createOrder(
                member,
                LocalDate.parse(orderRequest.getDate()),
                null,
                orderRequest.getTotalPrice()
        );

        // 6. 주문번호(UUID) 생성 후 설정
        order.setOrderCode(generateTossOrderId());

        // 7. 주문 항목 연결
        orderItems.forEach(order::addOrderItem);

        // 8. 저장
        orderRepository.save(order);

        // 9. 응답 DTO 반환
        return new OrderResponseDTO(
                order.getOrderCode(),      // UUID 기반 주문번호
                orderName,
                order.getTotalPrice(),
                member.getAccountEmail()
        );
    }

    // UUID 기반 주문번호 생성
    private String generateTossOrderId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public List<OrderHistoryResponseDTO> getOrdersByMemberId(Long memberId) {
        List<Order> orders = orderRepository.findByMemberIdAndOrderStatus(memberId, OrderStatus.SUCCESS);

        return orders.stream()
                .map(OrderHistoryResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public OrderDetailResponseDTO getOrderDetail(Long orderId, Long requesterId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));

        if (!order.getMember().getId().equals(requesterId)) {
            throw new AccessDeniedException("해당 주문에 접근할 수 없습니다.");
        }

        return OrderDetailResponseDTO.from(order);
    }

}
