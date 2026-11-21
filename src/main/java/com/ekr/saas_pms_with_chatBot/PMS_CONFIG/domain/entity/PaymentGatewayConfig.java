
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(
        name = "payment_gateway_config",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_gateway_property",
                        columnNames = "property_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentGatewayConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gateway_config_id")
    private Long gatewayConfigId;

    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "gateway_id",
            nullable = false
    )
    private PaymentGatewayCatalog gateway;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "property_id",
            nullable = false,
            unique = true
    )
    private Property property;

    // Stripe Secret Key đã mã hóa
    @Column(
            name = "secret_key",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String secretKey;

    // Stripe Webhook Secret đã mã hóa
    @Column(
            name = "webhook_secret",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String webhookSecret;
}