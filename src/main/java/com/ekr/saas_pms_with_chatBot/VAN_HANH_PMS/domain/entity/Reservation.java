package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "reservation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reservation_code",
                        columnNames = "reservation_code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_reservation_property_dates",
                        columnList = "property_id, check_in_date, check_out_date"
                ),
                @Index(
                        name = "idx_reservation_status_expiry",
                        columnList = "status, payment_expires_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "reservation_code", nullable = false, unique = true, length = 50)
    private String reservationCode;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReservationStatus status;

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Chỉ có giá trị khi Reservation cần thanh toán đặt cọc.
     */
    @Column(name = "payment_expires_at")
    private LocalDateTime paymentExpiresAt;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(
            name = "deposit_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "VND";

    /**
     * Lưu lại công thức đặt cọc và tham số tại thời điểm tạo Reservation.
     * Không bị ảnh hưởng nếu Property thay đổi Deposit Formula sau này.
     */
    @Column(
            name = "deposit_formula_snapshot",
            columnDefinition = "TEXT"
    )
    private String depositFormulaSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        if (depositAmount == null) {
            depositAmount = BigDecimal.ZERO;
        }

        if (currency == null) {
            currency = "VND";
        }
    }
}
