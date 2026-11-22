package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.adjust;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdjustDailyStatisticsRequest(
        @Min(0)
        Integer guestCount,

        @Min(0)
        Integer occupiedRoomCount,

        @Min(0)
        Integer availableRoomCount,

        @Min(0)
        Integer successfulReservationCount,

        @Min(0)
        Integer noShowCount,

        @Min(0)
        Integer cancelledReservationCount,

        @DecimalMin("0.00")
        BigDecimal depositAmount,

        @DecimalMin("0.00")
        BigDecimal depositCollected,

        @DecimalMin("0.00")
        BigDecimal refundTotal,

        @DecimalMin("0.00")
        BigDecimal expectedRoomRevenue,

        @DecimalMin("0.00")
        BigDecimal fnbRevenue,

        @DecimalMin("0.00")
        BigDecimal fnbExpense,

        @NotBlank(message = "Adjustment reason is required")
        @Size(
                max = 2000,
                message = "Adjustment reason must not exceed 2000 characters"
        )
        String adjustmentReason
) {

    public boolean hasAnyAdjustment() {
        return guestCount != null
                || occupiedRoomCount != null
                || availableRoomCount != null
                || successfulReservationCount != null
                || noShowCount != null
                || cancelledReservationCount != null
                || depositAmount != null
                || depositCollected != null
                || refundTotal != null
                || expectedRoomRevenue != null
                || fnbRevenue != null
                || fnbExpense != null;
    }
}
