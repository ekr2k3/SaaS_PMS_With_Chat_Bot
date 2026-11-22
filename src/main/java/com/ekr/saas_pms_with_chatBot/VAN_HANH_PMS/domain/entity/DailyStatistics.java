package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.User;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.DailyStatisticsStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "daily_statistics",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_statistics_property_date",
                        columnNames = {
                                "property_id",
                                "business_date"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_daily_statistics_business_date",
                        columnList = "business_date"
                ),
                @Index(
                        name = "idx_daily_statistics_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class DailyStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_statistics_id")
    private Long dailyStatisticsId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount = 0;

    @Column(name = "occupied_room_count", nullable = false)
    private Integer occupiedRoomCount = 0;

    @Column(name = "available_room_count", nullable = false)
    private Integer availableRoomCount = 0;

    @Column(name = "successful_reservation_count", nullable = false)
    private Integer successfulReservationCount = 0;

    @Column(name = "no_show_count", nullable = false)
    private Integer noShowCount = 0;

    @Column(name = "cancelled_reservation_count", nullable = false)
    private Integer cancelledReservationCount = 0;

    @Column(
            name = "deposit_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(
            name = "deposit_collected",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal depositCollected = BigDecimal.ZERO;

    @Column(
            name = "refund_total",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal refundTotal = BigDecimal.ZERO;

    @Column(
            name = "expected_room_revenue",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal expectedRoomRevenue = BigDecimal.ZERO;

    @Column(
            name = "fnb_revenue",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal fnbRevenue = BigDecimal.ZERO;

    @Column(
            name = "fnb_expense",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal fnbExpense = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private DailyStatisticsStatus status =
            DailyStatisticsStatus.REVIEWING;

    @Column(
            name = "adjustment_reason",
            columnDefinition = "TEXT"
    )
    private String adjustmentReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = DailyStatisticsStatus.REVIEWING;
        }

        if (guestCount == null) guestCount = 0;
        if (occupiedRoomCount == null) occupiedRoomCount = 0;
        if (availableRoomCount == null) availableRoomCount = 0;
        if (successfulReservationCount == null) {
            successfulReservationCount = 0;
        }
        if (noShowCount == null) noShowCount = 0;
        if (cancelledReservationCount == null) {
            cancelledReservationCount = 0;
        }

        if (depositAmount == null) depositAmount = BigDecimal.ZERO;
        if (depositCollected == null) depositCollected = BigDecimal.ZERO;
        if (refundTotal == null) refundTotal = BigDecimal.ZERO;
        if (expectedRoomRevenue == null) {
            expectedRoomRevenue = BigDecimal.ZERO;
        }
        if (fnbRevenue == null) fnbRevenue = BigDecimal.ZERO;
        if (fnbExpense == null) fnbExpense = BigDecimal.ZERO;
    }
}
