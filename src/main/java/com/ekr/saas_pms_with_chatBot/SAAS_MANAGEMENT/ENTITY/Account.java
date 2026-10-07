package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_account_email",
                        columnNames = "email"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(
            name = "email",
            nullable = false,
            unique = true,
            length = 255
    )
    private String email;

    @Column(
            name = "password",
            nullable = false,
            length = 255
    )
    private String password;

    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private AccountStatus status;


    // =========================
    // ENUM
    // =========================

    public enum AccountStatus {
        ACTIVE,
        INACTIVE
    }


    // Để bảo  dam quan he 2 chieu
    @OneToOne(mappedBy = "account")
    private Tenant tenant;
}
