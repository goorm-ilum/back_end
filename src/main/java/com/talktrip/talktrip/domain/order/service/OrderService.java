package com.talktrip.talktrip.domain.order.service;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.order.dto.request.OrderRequestDTO;
import com.talktrip.talktrip.domain.order.dto.response.OrderResponseDTO;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.entity.OrderItem;
import com.talktrip.talktrip.domain.order.repository.OrderRepository;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.product.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Random;
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

        // 4. 주문명 생성 (옵션 수량 기준)
        int totalQuantity = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();
        String orderName = (totalQuantity == 1)
                ? product.getProductName() + " - 단일 옵션"
                : product.getProductName() + " 외 " + (totalQuantity - 1) + "건";

        // 5. 주문 생성 (결제 수단은 null, 결제 시 설정 예정)
        Order order = Order.createOrder(
                member,
                LocalDate.parse(orderRequest.getDate()),
                null,
                orderRequest.getTotalPrice()
        );

        // 6. 주문 항목 연결
        orderItems.forEach(order::addOrderItem);

        // 7. 저장
        orderRepository.save(order);

        // 8. 토스 페이먼츠 규칙에 맞는 orderId 생성
        String tossOrderId = generateTossOrderId(order.getId());

        // 9. 응답 DTO 반환
        return new OrderResponseDTO(
                tossOrderId,  // order.getId().toString() 대신
                orderName,
                order.getTotalPrice(),
                member.getAccountEmail()
        );
    }

    // 토스 페이먼츠 규칙에 맞는 orderId 생성 메서드 추가
    private String generateTossOrderId(Long orderId) {
        // 방법 1: UUID 사용 (32자)
        return UUID.randomUUID().toString().replace("-", "");

        // 방법 2: 타임스탬프 + 랜덤 문자열 (더 짧고 읽기 쉬움)
        // return System.currentTimeMillis() + "_" + generateRandomString(6);

        // 방법 3: 주문ID + 타임스탬프 조합
        // return "ORDER_" + orderId + "_" + System.currentTimeMillis();
    }

    // 랜덤 문자열 생성 메서드 (방법 2 사용 시)
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}