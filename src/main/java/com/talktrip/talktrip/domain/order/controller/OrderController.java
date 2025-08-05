package com.talktrip.talktrip.domain.order.controller;

import com.talktrip.talktrip.domain.order.dto.request.OrderRequestDTO;
import com.talktrip.talktrip.domain.order.dto.response.OrderResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.OrderHistoryResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.OrderDetailResponseDTO;
import com.talktrip.talktrip.domain.order.service.OrderService;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

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

    @Operation(summary = "주문 조회", description = "로그인한 사용자의 주문내역을 반환합니다.")
    @GetMapping("/orders/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal CustomMemberDetails memberDetails) {
        if (memberDetails == null) {
            return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");
        }

        Long memberId = memberDetails.getId();

        List<OrderHistoryResponseDTO> myOrders = orderService.getOrdersByMemberId(memberId);

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