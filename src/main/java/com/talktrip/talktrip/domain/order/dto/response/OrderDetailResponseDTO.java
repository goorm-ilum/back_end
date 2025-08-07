package com.talktrip.talktrip.domain.order.dto.response;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.entity.OrderItem;
import com.talktrip.talktrip.domain.order.entity.Payment;
import com.talktrip.talktrip.domain.order.entity.CardPayment;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class OrderDetailResponseDTO {

    private final String orderId;
    private final LocalDateTime orderCreatedAt; // 주문일 (createdAt)
    private final LocalDate useDate; // 사용일 (orderDate)
    private final String paymentMethod;
    private final int totalPrice;
    private final String orderStatus;

    private final MemberInfoDTO member;
    private final List<OrderItemDTO> orderItems;
    private final PaymentInfoDTO paymentInfo; // 새로운 결제 정보

    // 기존 Order 엔티티를 받는 생성자 (하위 호환성)
    public OrderDetailResponseDTO(Order order) {
        this.orderId = order.getOrderCode();
        this.orderCreatedAt = order.getCreatedAt();
        this.useDate = order.getOrderDate();
        
        // Payment 정보가 있으면 Payment에서 가져오고, 없으면 기본값 사용
        if (order.getPayment() != null && order.getPayment().getMethod() != null) {
            this.paymentMethod = order.getPayment().getMethod().name();
        } else {
            // Payment 정보가 없는 경우 기본값 설정
            this.paymentMethod = "CARD"; // 기본값으로 카드 설정
        }
        
        this.totalPrice = order.getTotalPrice();
        this.orderStatus = order.getOrderStatus().name();

        this.member = new MemberInfoDTO(order.getMember());
        this.orderItems = order.getOrderItems().stream()
                .map(OrderItemDTO::new)
                .collect(Collectors.toList());
        
        // Payment 정보가 있으면 PaymentInfoDTO 생성
        this.paymentInfo = order.getPayment() != null 
            ? new PaymentInfoDTO(order.getPayment()) : null;
    }

    // 새로운 복합 데이터를 받는 생성자
    public OrderDetailResponseDTO(String orderId, LocalDateTime orderCreatedAt, LocalDate useDate,
                                 String paymentMethod, int totalPrice, String orderStatus,
                                 MemberInfoDTO member, List<OrderItemDTO> orderItems, 
                                 PaymentInfoDTO paymentInfo) {
        this.orderId = orderId;
        this.orderCreatedAt = orderCreatedAt;
        this.useDate = useDate;
        this.paymentMethod = paymentMethod;
        this.totalPrice = totalPrice;
        this.orderStatus = orderStatus;
        this.member = member;
        this.orderItems = orderItems;
        this.paymentInfo = paymentInfo;
    }

    @Getter
    public static class MemberInfoDTO {
        private final String name;
        private final String email;
        private final String phone;

        public MemberInfoDTO(Member member) {
            this.name = member.getName();
            this.email = member.getAccountEmail();
            this.phone = member.getPhoneNum();
        }

        public MemberInfoDTO(String name, String email, String phone) {
            this.name = name;
            this.email = email;
            this.phone = phone;
        }
    }

    @Getter
    public static class OrderItemDTO {
        private final Long id;
        private final String productName;
        private final String productThumbnail;
        private final String optionName;
        private final int quantity;
        private final int unitPrice;
        private final int totalItemPrice;
        private final LocalDate startDate;

        public OrderItemDTO(OrderItem orderItem) {
            // 스냅샷 데이터 사용
            this.id = orderItem.getId();
            this.productName = orderItem.getProductName();
            this.productThumbnail = orderItem.getProductThumbnailUrl();
            this.optionName = orderItem.getOptionName();
            this.quantity = orderItem.getQuantity();
            this.unitPrice = orderItem.getPrice();
            this.totalItemPrice = orderItem.getPrice() * orderItem.getQuantity();
            this.startDate = orderItem.getStartDate();
        }
    }

    @Getter
    public static class PaymentInfoDTO {
        private final String paymentMethod;
        private final String paymentKey;
        private final LocalDateTime approvedAt;
        private final String receiptUrl;
        private final String status;
        private final int totalAmount;
        private final int vat;
        private final int suppliedAmount;
        private final boolean isPartialCancelable;
        private final CardInfoDTO cardInfo; // 카드 결제인 경우만
        
        // 상세 결제 정보 추가
        private final String easyPayProvider; // 카카오페이, 네이버페이 등
        private final String cardCompany; // 신한카드, 삼성카드 등
        private final String accountBank; // 신한은행, 국민은행 등

        public PaymentInfoDTO(Payment payment) {
            this.paymentMethod = payment.getMethod() != null ? payment.getMethod().name() : null;
            this.paymentKey = payment.getPaymentKey();
            this.approvedAt = payment.getApprovedAt();
            this.receiptUrl = payment.getReceiptUrl();
            this.status = payment.getStatus();
            this.totalAmount = payment.getTotalAmount();
            this.vat = payment.getVat();
            this.suppliedAmount = payment.getSuppliedAmount();
            this.isPartialCancelable = payment.isPartialCancelable();
            this.cardInfo = payment.getCardPayment() != null 
                ? new CardInfoDTO(payment.getCardPayment()) : null;
            this.easyPayProvider = payment.getEasyPayProvider();
            this.cardCompany = payment.getCardCompany();
            this.accountBank = payment.getAccountBank();
        }
    }

    @Getter
    public static class CardInfoDTO {
        private final String cardNumber;
        private final String issuerCode;
        private final String acquirerCode;
        private final String approveNo;
        private final int installmentMonths;
        private final boolean isInterestFree;
        private final String cardType;
        private final String ownerType;
        private final String acquireStatus;
        private final int amount;

        public CardInfoDTO(CardPayment cardPayment) {
            this.cardNumber = cardPayment.getCardNumber();
            this.issuerCode = cardPayment.getIssuerCode();
            this.acquirerCode = cardPayment.getAcquirerCode();
            this.approveNo = cardPayment.getApproveNo();
            this.installmentMonths = cardPayment.getInstallmentMonths();
            this.isInterestFree = cardPayment.isInterestFree();
            this.cardType = cardPayment.getCardType();
            this.ownerType = cardPayment.getOwnerType();
            this.acquireStatus = cardPayment.getAcquireStatus();
            this.amount = cardPayment.getAmount();
        }
    }

    public static OrderDetailResponseDTO from(Order order) {
        return new OrderDetailResponseDTO(order);
    }

    public static OrderDetailResponseDTO from(String orderId, LocalDateTime orderCreatedAt, 
                                             LocalDate useDate, String paymentMethod, int totalPrice, 
                                             String orderStatus, MemberInfoDTO member, 
                                             List<OrderItemDTO> orderItems, PaymentInfoDTO paymentInfo) {
        return new OrderDetailResponseDTO(orderId, orderCreatedAt, useDate, paymentMethod, 
                                        totalPrice, orderStatus, member, orderItems, paymentInfo);
    }
}

