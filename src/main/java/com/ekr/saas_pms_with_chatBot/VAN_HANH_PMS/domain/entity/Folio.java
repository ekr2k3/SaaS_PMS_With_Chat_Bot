package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(
        name = "folio",
        indexes = {
                @Index(
                        name = "idx_folio_reservation",
                        columnList = "reservation_id"
                ),
                @Index(
                        name = "idx_folio_guest",
                        columnList = "guest_id"
                ),
                @Index(
                        name = "idx_folio_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Folio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folio_id")
    private Long folioId;

    @Column(
            name = "service",
            nullable = false,
            length = 50
    )
    private String service;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(
            name = "unit_price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal unitPrice;

    @Column(
            name = "amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "service_data",
            columnDefinition = "json"
    )
    private String serviceData;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reservation_id",
            nullable = false
    )
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "guest_id",
            nullable = false
    )
    private Guest guest;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(
                    ZoneId.of("Asia/Ho_Chi_Minh")
            );
        }
    }
}