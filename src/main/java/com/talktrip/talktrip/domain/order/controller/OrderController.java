package com.talktrip.talktrip.domain.order.controller;

import com.talktrip.talktrip.domain.order.dto.request.OrderRequestDTO;
import com.talktrip.talktrip.domain.order.dto.response.OrderResponseDTO;
import com.talktrip.talktrip.domain.order.service.OrderService;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Order", description = "주문 관련 API")
@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "주문 생성")
    @PostMapping("/orders/{productId}")
    public ResponseEntity<OrderResponseDTO> createOrder(
            @PathVariable Long productId,
            @RequestBody OrderRequestDTO orderRequest,
            @AuthenticationPrincipal CustomMemberDetails memberDetails
    ) {
        Long memberId = memberDetails.getId(); // JWT에서 파싱된 사용자 ID

        // 주문 생성 요청을 서비스로 위임
        OrderResponseDTO orderResponse = orderService.createOrder(productId, orderRequest, memberId);

        return ResponseEntity.ok(orderResponse);
    }
}