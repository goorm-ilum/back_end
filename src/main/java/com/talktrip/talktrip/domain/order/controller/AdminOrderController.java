package com.talktrip.talktrip.domain.order.controller;

import com.talktrip.talktrip.domain.order.dto.response.AdminOrderResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderDetailResponseDTO;
import com.talktrip.talktrip.domain.order.service.AdminOrderService;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Order", description = "주문 관련 API")
@RestController
@RequestMapping("api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @Operation(summary = "어드민 주문 조회", description = "어드민 사용자의 주문내역을 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<List<AdminOrderResponseDTO>> getOrdersBySeller(@AuthenticationPrincipal CustomMemberDetails memberDetails) {
        List<AdminOrderResponseDTO> orders = adminOrderService.getOrdersBySeller(memberDetails);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "어드민 주문 상세조회", description = "어드민 사용자의 상세 주문내역을 반환합니다.")
    @GetMapping("/{orderCode}")
    public ResponseEntity<AdminOrderDetailResponseDTO> getOrderDetail(
            @PathVariable String orderCode,
            @AuthenticationPrincipal CustomMemberDetails memberDetails) {
        AdminOrderDetailResponseDTO orderDetail = adminOrderService.getOrderDetail(orderCode, memberDetails);
        return ResponseEntity.ok(orderDetail);
    }
}

