package com.talktrip.talktrip.domain.order.entity;

import com.talktrip.talktrip.domain.order.enums.PaymentMethod;
import com.talktrip.talktrip.domain.order.enums.PaymentProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "method")
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private PaymentProvider provider;

    @Column(name = "payment_key", nullable = false, unique = true)
    private String paymentKey;

    @Column(name = "status")
    private String status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "total_amount")
    private int totalAmount;

    @Column(name = "vat")
    private int vat;

    @Column(name = "supplied_amount")
    private int suppliedAmount;

    @Column(name = "receipt_url")
    private String receiptUrl;

    @Column(name = "is_partial_cancelable")
    private boolean isPartialCancelable;

    // 상세 결제 정보 추가
    @Column(name = "easy_pay_provider")
    private String easyPayProvider; // 카카오페이, 네이버페이, 페이코 등

    @Column(name = "card_company")
    private String cardCompany; // 신한카드, 삼성카드, 현대카드 등

    @Column(name = "account_bank")
    private String accountBank; // 신한은행, 국민은행 등

    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL)
    private CardPayment cardPayment;

    public void setOrder(Order order) {
        this.order = order;
    }

    public void setCardPayment(CardPayment cardPayment) {
        this.cardPayment = cardPayment;
    }

    public static Payment createPayment(Order order,
                                        String paymentKey,
                                        PaymentMethod method,
                                        PaymentProvider provider,
                                        int totalAmount,
                                        int vat,
                                        int suppliedAmount,
                                        String status,
                                        LocalDateTime approvedAt,
                                        String receiptUrl,
                                        boolean isPartialCancelable,
                                        String easyPayProvider,
                                        String cardCompany,
                                        String accountBank) {
        Payment payment = new Payment();
        payment.order = order;
        payment.paymentKey = paymentKey;
        payment.method = method;
        payment.provider = provider;
        payment.totalAmount = totalAmount;
        payment.vat = vat;
        payment.suppliedAmount = suppliedAmount;
        payment.status = status;
        payment.approvedAt = approvedAt;
        payment.receiptUrl = receiptUrl;
        payment.isPartialCancelable = isPartialCancelable;
        payment.easyPayProvider = easyPayProvider;
        payment.cardCompany = cardCompany;
        payment.accountBank = accountBank;
        return payment;
    }
}


