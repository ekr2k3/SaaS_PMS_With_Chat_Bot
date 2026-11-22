package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.view;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.DailyStatistics;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.DailyStatisticsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class ViewDailyStatisticsService {

    private final DailyStatisticsRepository dailyStatisticsRepository;
    private final ViewDailyStatisticsAccessService accessService;

    public ViewDailyStatisticsService(
            DailyStatisticsRepository dailyStatisticsRepository,
            ViewDailyStatisticsAccessService accessService
    ) {
        this.dailyStatisticsRepository = dailyStatisticsRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public List<ViewDailyStatisticsResponse> getDailyStatistics(
            Long propertyId,
            ViewDailyStatisticsRequest request
    ) {
        accessService.requireAccess(propertyId);

        LocalDate fromDate = request.fromDate();
        LocalDate toDate = request.toDate();

        if (fromDate == null || toDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "fromDate and toDate are required"
            );
        }

        if (fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "fromDate must not be after toDate"
            );
        }

        List<DailyStatistics> statistics =
                dailyStatisticsRepository
                        .findAllByProperty_PropertyIdAndBusinessDateBetweenOrderByBusinessDateDesc(
                                propertyId,
                                fromDate,
                                toDate
                        );

        return statistics.stream()
                .map(ViewDailyStatisticsResponse::from)
                .toList();
    }
}
