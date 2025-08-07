package com.talktrip.talktrip.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "card_payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "card_number")
    private String cardNumber;

    @Column(name = "issuer_code")
    private String issuerCode;

    @Column(name = "acquirer_code")
    private String acquirerCode;

    @Column(name = "approve_no")
    private String approveNo;

    @Column(name = "installment_months")
    private int installmentMonths;

    @Column(name = "is_interest_free")
    private boolean isInterestFree;

    @Column(name = "card_type")
    private String cardType; // 신용, 체크 등

    @Column(name = "owner_type")
    private String ownerType; // 개인, 법인

    @Column(name = "acquire_status")
    private String acquireStatus;

    @Column(name = "amount")
    private int amount;

    public static CardPayment createCardPayment(Payment payment, String cardNumber, String issuerCode, String acquirerCode, String approveNo, int installmentMonths, boolean isInterestFree, String cardType, String ownerType, String acquireStatus, int amount) {
        CardPayment cp = new CardPayment();
        cp.payment = payment;
        cp.cardNumber = cardNumber;
        cp.issuerCode = issuerCode;
        cp.acquirerCode = acquirerCode;
        cp.approveNo = approveNo;
        cp.installmentMonths = installmentMonths;
        cp.isInterestFree = isInterestFree;
        cp.cardType = cardType;
        cp.ownerType = ownerType;
        cp.acquireStatus = acquireStatus;
        cp.amount = amount;
        return cp;
    }
}

