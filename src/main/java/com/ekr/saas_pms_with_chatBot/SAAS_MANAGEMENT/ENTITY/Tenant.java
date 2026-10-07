package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tenant",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tenant_account",
                        columnNames = "account_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status;

    @OneToOne
    @JoinColumn(
            name = "account_id",
            nullable = false,
            unique = true
    )
    private Account account;

    public enum TenantStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }

    // Dam bao quan he 2 chieu
    @OneToOne(mappedBy = "tenant")
    private Subscription subscription;
}
