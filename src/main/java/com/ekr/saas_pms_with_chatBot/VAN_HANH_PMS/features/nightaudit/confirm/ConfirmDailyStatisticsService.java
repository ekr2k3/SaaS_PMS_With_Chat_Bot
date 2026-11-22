package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.confirm;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.User;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.UserRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.DailyStatistics;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.DailyStatisticsStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.DailyStatisticsRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.view.ViewDailyStatisticsResponse;
import com.ekr.saas_pms_with_chatBot.filter.PmsEmployeePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class ConfirmDailyStatisticsService {

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final DailyStatisticsRepository dailyStatisticsRepository;
    private final UserRepository userRepository;
    private final ConfirmDailyStatisticsAccessService accessService;

    public ConfirmDailyStatisticsService(
            DailyStatisticsRepository dailyStatisticsRepository,
            UserRepository userRepository,
            ConfirmDailyStatisticsAccessService accessService
    ) {
        this.dailyStatisticsRepository = dailyStatisticsRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    @Transactional
    public ConfirmDailyStatisticsResponse confirm(
            Long propertyId,
            LocalDate businessDate
    ) {
        PmsEmployeePrincipal employee =
                accessService.requireAccess(propertyId);

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
                    "Only REVIEWING Daily Statistics can be confirmed"
            );
        }

        User manager = userRepository.findById(employee.userId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Manager account not found"
                ));

        statistics.setStatus(DailyStatisticsStatus.CONFIRMED);
        statistics.setConfirmedBy(manager);
        statistics.setConfirmedAt(
                LocalDateTime.now(BUSINESS_ZONE)
        );

        DailyStatistics saved =
                dailyStatisticsRepository.save(statistics);

        return new ConfirmDailyStatisticsResponse(
                "Night Audit confirmed successfully",
                ViewDailyStatisticsResponse.from(saved)
        );
    }
}
