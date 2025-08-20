package com.talktrip.talktrip.domain.order.controller;

import com.talktrip.talktrip.domain.order.dto.request.OrderRequestDTO;
import com.talktrip.talktrip.domain.order.dto.response.OrderDetailResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.OrderHistoryResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.OrderResponseDTO;
import com.talktrip.talktrip.domain.order.service.OrderService;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Order", description = "주문 관련 API")
@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "주문 생성", description = "주문을 생성해서 생성된 정보를 반환합니다.")
    @PostMapping("/orders/{productId}")
    public ResponseEntity<OrderResponseDTO> createOrder(
            @PathVariable Long productId,
            @RequestBody OrderRequestDTO orderRequest,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        Long memberId = memberDetails.getId();

        OrderResponseDTO orderResponse = orderService.createOrder(productId, orderRequest, memberId);

        return ResponseEntity.ok(orderResponse);
    }

    @Operation(summary = "주문 조회", description = "로그인한 사용자의 주문내역을 페이지네이션과 함께 반환합니다.")
    @GetMapping("/orders/me")
    public ResponseEntity<Page<OrderHistoryResponseDTO>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        if (memberDetails == null) {
            return ResponseEntity.status(401).build();
        }

        Long memberId = memberDetails.getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderHistoryResponseDTO> myOrders = orderService.getOrdersByMemberIdWithPagination(memberId, pageable);

        return ResponseEntity.ok(myOrders);
    }

    @Operation(summary = "주문 상세 조회", description = "주문의 상세 내역을 반환합니다.")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDetailResponseDTO> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        OrderDetailResponseDTO detail = orderService.getOrderDetail(orderId, memberDetails.getId());
        return ResponseEntity.ok(detail);
    }

}