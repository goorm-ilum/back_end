package com.talktrip.talktrip.domain.order.controller;

import com.talktrip.talktrip.domain.order.dto.response.AdminOrderDetailResponseDTO;
import com.talktrip.talktrip.domain.order.dto.response.AdminOrderResponseDTO;
import com.talktrip.talktrip.domain.order.service.AdminOrderService;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Order", description = "주문 관련 API")
@RestController
@RequestMapping("api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @Operation(summary = "어드민 주문 조회", description = "어드민 사용자의 주문내역을 페이지네이션과 필터링과 함께 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<Page<AdminOrderResponseDTO>> getOrdersBySeller(
            @AuthenticationPrincipal CustomMemberDetails memberDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orderStatus) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminOrderResponseDTO> orders = adminOrderService.getOrdersBySellerWithPagination(
                memberDetails, pageable, sort, paymentMethod, keyword, orderStatus);
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

