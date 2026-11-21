package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "payment_gateway_catalog",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_gateway_name",
                        columnNames = "name"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentGatewayCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gateway_id")
    private Long gatewayId;

    @Column(
            name = "name",
            nullable = false,
            unique = true,
            length = 100
    )
    private String name;

    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;

    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private String status;
}
