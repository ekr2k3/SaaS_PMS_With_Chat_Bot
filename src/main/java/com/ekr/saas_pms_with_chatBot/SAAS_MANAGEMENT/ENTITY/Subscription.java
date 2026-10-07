package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "subscription",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subscription_tenant",
                        columnNames = "tenant_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @OneToOne
    @JoinColumn(
            name = "tenant_id",
            nullable = false,
            unique = true
    )
    private Tenant tenant;

    public enum SubscriptionStatus {
        PENDING,
        ACTIVE,
        EXPIRED
    }
}
