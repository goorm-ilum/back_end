package com.talktrip.talktrip.domain.order.service;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.order.dto.request.OrderRequestDTO;
import com.talktrip.talktrip.domain.order.dto.response.OrderResponseDTO;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.entity.OrderItem;
import com.talktrip.talktrip.domain.order.enums.PaymentMethod;
import com.talktrip.talktrip.domain.order.repository.OrderRepository;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.product.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;

    public OrderResponseDTO createOrder(OrderRequestDTO orderRequest) {

        // 1. 상품 조회
        Product product = productRepository.findById(orderRequest.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        // 2. 회원 조회 (실제로는 SecurityContext에서 가져와야 함)
        Member member = memberRepository.findById(orderRequest.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        // 3. 옵션명을 옵션 ID로 변환
        List<OrderItem> orderItems = orderRequest.getOptions().stream()
                .map(optReq -> {
                    ProductOption productOption = productOptionRepository.findByProductIdAndOptionName(
                            orderRequest.getProductId(), optReq.getOptionName())
                            .orElseThrow(() -> new IllegalArgumentException("옵션을 찾을 수 없습니다: " + optReq.getOptionName()));
                    
                    return OrderItem.createOrderItem(
                            productOption.getProduct(),
                            productOption,
                            optReq.getQuantity(),
                            productOption.getExtraPrice() * optReq.getQuantity()
                    );
                })
                .collect(Collectors.toList());

        // 4. 주문명 생성
        int totalQuantity = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();
        String orderName = (totalQuantity == 1)
                ? product.getProductName() + " - 단일 옵션"
                : product.getProductName() + " 외 " + (totalQuantity - 1) + "건";

        // 5. 주문 생성 (결제 수단은 나중에 결제 시점에 설정)
        Order order = Order.createOrder(
                member,
                LocalDate.parse(orderRequest.getDate()),
                null, // 결제 수단은 결제 시점에 설정
                orderRequest.getTotalPrice()
        );

        // 6. 주문 항목 연결
        for (OrderItem item : orderItems) {
            order.addOrderItem(item);
        }

        // 7. 저장 (Cascade 설정 시 OrderItem도 함께 저장됨)
        orderRepository.save(order);

        return new OrderResponseDTO(
                order.getId().toString(),
                orderName,
                order.getTotalPrice(),
                member.getAccountEmail()
        );
    }
}
