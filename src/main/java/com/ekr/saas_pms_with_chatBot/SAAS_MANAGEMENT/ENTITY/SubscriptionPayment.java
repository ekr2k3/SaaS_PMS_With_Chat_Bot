package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentGateway;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(
        name = "SUBSCRIPTION_PAYMENT",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_subscription_payment_order",
                        columnNames = "order_id"
                ),
                @UniqueConstraint(
                        name = "uq_subscription_payment_request",
                        columnNames = {
                                "payment_gateway",
                                "request_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uq_subscription_payment_transaction",
                        columnNames = {
                                "payment_gateway",
                                "transaction_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_subscription_payment_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_subscription_payment_subscription",
                        columnList = "subscription_id"
                )
        }
)
public class SubscriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "order_id",
            nullable = false,
            unique = true,
            length = 100
    )
    private String orderId;

    @Column(
            name = "request_id",
            nullable = false,
            length = 100
    )
    private String requestId;

    @Column(
            name = "transaction_id",
            length = 255
    )
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_gateway",
            nullable = false,
            length = 50
    )
    private PaymentGateway paymentGateway;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal amount;

    @Column(
            name = "currency",
            nullable = false,
            length = 3
    )
    private String currency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    private Map<String, Object> metadata;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "subscription_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_subscription_payment_subscription"
            )
    )
    private Subscription subscription;

    public SubscriptionPayment() {
    }

    // Generate getters and setters using IntelliJ.
}
