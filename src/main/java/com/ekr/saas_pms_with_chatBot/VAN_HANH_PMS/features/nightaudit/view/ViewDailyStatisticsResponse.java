package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.view;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.DailyStatistics;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.DailyStatisticsStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ViewDailyStatisticsResponse(
        Long dailyStatisticsId,
        Long propertyId,
        LocalDate businessDate,
        Integer guestCount,
        Integer occupiedRoomCount,
        Integer availableRoomCount,
        Integer successfulReservationCount,
        Integer noShowCount,
        Integer cancelledReservationCount,
        BigDecimal depositAmount,
        BigDecimal depositCollected,
        BigDecimal refundTotal,
        BigDecimal expectedRoomRevenue,
        BigDecimal fnbRevenue,
        BigDecimal fnbExpense,
        DailyStatisticsStatus status,
        String adjustmentReason,
        Long confirmedBy,
        String confirmedByName,
        LocalDateTime confirmedAt
) {

    public static ViewDailyStatisticsResponse from(
            DailyStatistics statistics
    ) {
        return new ViewDailyStatisticsResponse(
                statistics.getDailyStatisticsId(),
                statistics.getProperty().getPropertyId(),
                statistics.getBusinessDate(),
                statistics.getGuestCount(),
                statistics.getOccupiedRoomCount(),
                statistics.getAvailableRoomCount(),
                statistics.getSuccessfulReservationCount(),
                statistics.getNoShowCount(),
                statistics.getCancelledReservationCount(),
                statistics.getDepositAmount(),
                statistics.getDepositCollected(),
                statistics.getRefundTotal(),
                statistics.getExpectedRoomRevenue(),
                statistics.getFnbRevenue(),
                statistics.getFnbExpense(),
                statistics.getStatus(),
                statistics.getAdjustmentReason(),
                statistics.getConfirmedBy() == null
                        ? null
                        : statistics.getConfirmedBy().getUserId(),
                statistics.getConfirmedBy() == null
                        ? null
                        : statistics.getConfirmedBy().getFullName(),
                statistics.getConfirmedAt()
        );
    }
}
