package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.generate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyStatisticsScheduler {

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final GenerateDailyStatisticsService generateService;

    /**
     * Chạy hằng ngày lúc 17:00 theo giờ Việt Nam.
     */
    @Scheduled(
            cron = "${night-audit.scheduler.cron:0 0 17 * * *}",
            zone = "${night-audit.scheduler.zone:Asia/Ho_Chi_Minh}"
    )
    public void generateDailyStatistics() {
        LocalDate businessDate = LocalDate.now(BUSINESS_ZONE);

        log.info(
                "Bắt đầu tạo Night Audit Daily Statistics: businessDate={}",
                businessDate
        );

        try {
            generateService.generateForAllProperties(businessDate);

            log.info(
                    "Scheduler Night Audit hoàn thành: businessDate={}",
                    businessDate
            );
        } catch (Exception exception) {
            log.error(
                    "Scheduler Night Audit thất bại: businessDate={}",
                    businessDate,
                    exception
            );
        }
    }
}
