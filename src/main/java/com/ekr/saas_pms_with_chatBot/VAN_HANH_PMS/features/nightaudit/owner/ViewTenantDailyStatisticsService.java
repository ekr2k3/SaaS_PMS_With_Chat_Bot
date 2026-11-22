package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.owner;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.DailyStatistics;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.DailyStatisticsRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.view.ViewDailyStatisticsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class ViewTenantDailyStatisticsService {

    private final DailyStatisticsRepository dailyStatisticsRepository;
    private final ViewTenantDailyStatisticsAccessService accessService;

    public ViewTenantDailyStatisticsService(
            DailyStatisticsRepository dailyStatisticsRepository,
            ViewTenantDailyStatisticsAccessService accessService
    ) {
        this.dailyStatisticsRepository = dailyStatisticsRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public ViewTenantDailyStatisticsResponse view(
            Long propertyId,
            LocalDate businessDate,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Tenant tenant = accessService.requireOwnerTenant();

        if ((fromDate == null) != (toDate == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "fromDate and toDate must be provided together"
            );
        }

        if (businessDate != null
                && (fromDate != null || toDate != null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use businessDate or date range, not both"
            );
        }

        if (fromDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "fromDate must not be after toDate"
            );
        }

        List<DailyStatistics> reports =
                dailyStatisticsRepository.findAllForTenantOwner(
                        tenant.getTenantId(),
                        propertyId,
                        businessDate,
                        fromDate,
                        toDate
                );

        List<ViewDailyStatisticsResponse> responseList = reports.stream()
                .map(ViewDailyStatisticsResponse::from)
                .toList();

        return new ViewTenantDailyStatisticsResponse(
                tenant.getTenantId(),
                responseList.size(),
                responseList
        );
    }
}
