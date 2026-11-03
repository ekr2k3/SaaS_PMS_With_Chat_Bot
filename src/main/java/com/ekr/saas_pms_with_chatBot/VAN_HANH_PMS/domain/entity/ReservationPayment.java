package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "reservation_payment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reservation_payment_checkout_session",
                        columnNames = "checkout_session_id"
                ),
                @UniqueConstraint(
                        name = "uk_reservation_payment_intent",
                        columnNames = "payment_intent_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_reservation_payment_reservation",
                        columnList = "reservation_id"
                ),
                @Index(
                        name = "idx_reservation_payment_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ReservationPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_payment_id")
    private Long reservationPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private ReservationPaymentType paymentType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReservationPaymentStatus status = ReservationPaymentStatus.PENDING;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "gateway_refund_id", length = 255)
    private String gatewayRefundId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "checkout_session_id", length = 255)
    private String checkoutSessionId;

    @Column(name = "payment_intent_id", length = 255)
    private String paymentIntentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;
}
