package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.adjust;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.DailyStatistics;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.DailyStatisticsStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.DailyStatisticsRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.view.ViewDailyStatisticsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class AdjustDailyStatisticsService {

    private final DailyStatisticsRepository dailyStatisticsRepository;
    private final AdjustDailyStatisticsAccessService accessService;

    public AdjustDailyStatisticsService(
            DailyStatisticsRepository dailyStatisticsRepository,
            AdjustDailyStatisticsAccessService accessService
    ) {
        this.dailyStatisticsRepository = dailyStatisticsRepository;
        this.accessService = accessService;
    }

    @Transactional
    public AdjustDailyStatisticsResponse adjust(
            Long propertyId,
            LocalDate businessDate,
            AdjustDailyStatisticsRequest request
    ) {
        accessService.requireAccess(propertyId);

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request body is required"
            );
        }

        DailyStatistics statistics =
                dailyStatisticsRepository
                        .findByPropertyAndBusinessDateForUpdate(
                                propertyId,
                                businessDate
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Daily Statistics not found"
                        ));

        if (statistics.getStatus()
                != DailyStatisticsStatus.REVIEWING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Confirmed Daily Statistics cannot be adjusted"
            );
        }

        if (!request.hasAnyAdjustment()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one statistic must be provided"
            );
        }

        applyAdjustments(statistics, request);
        statistics.setAdjustmentReason(request.adjustmentReason().trim());

        DailyStatistics saved =
                dailyStatisticsRepository.save(statistics);

        return new AdjustDailyStatisticsResponse(
                "Daily Statistics adjusted successfully",
                ViewDailyStatisticsResponse.from(saved)
        );
    }

    private void applyAdjustments(
            DailyStatistics statistics,
            AdjustDailyStatisticsRequest request
    ) {
        if (request.guestCount() != null) {
            statistics.setGuestCount(request.guestCount());
        }
        if (request.occupiedRoomCount() != null) {
            statistics.setOccupiedRoomCount(request.occupiedRoomCount());
        }
        if (request.availableRoomCount() != null) {
            statistics.setAvailableRoomCount(request.availableRoomCount());
        }
        if (request.successfulReservationCount() != null) {
            statistics.setSuccessfulReservationCount(
                    request.successfulReservationCount()
            );
        }
        if (request.noShowCount() != null) {
            statistics.setNoShowCount(request.noShowCount());
        }
        if (request.cancelledReservationCount() != null) {
            statistics.setCancelledReservationCount(
                    request.cancelledReservationCount()
            );
        }
        if (request.depositAmount() != null) {
            statistics.setDepositAmount(request.depositAmount());
        }
        if (request.depositCollected() != null) {
            statistics.setDepositCollected(request.depositCollected());
        }
        if (request.refundTotal() != null) {
            statistics.setRefundTotal(request.refundTotal());
        }
        if (request.expectedRoomRevenue() != null) {
            statistics.setExpectedRoomRevenue(
                    request.expectedRoomRevenue()
            );
        }
        if (request.fnbRevenue() != null) {
            statistics.setFnbRevenue(request.fnbRevenue());
        }
        if (request.fnbExpense() != null) {
            statistics.setFnbExpense(request.fnbExpense());
        }
    }
}
