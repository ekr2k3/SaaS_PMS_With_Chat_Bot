package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpirationScheduler {

    private final SubscriptionExpirationService expirationService;

    /**
     * Kích hoạt tiến trình 1.2F09 theo lịch cấu hình.
     */
    @Scheduled(
            cron = "${subscription.expiration-check.cron:0 5 0 * * *}",
            zone = "${subscription.expiration-check.zone:Asia/Ho_Chi_Minh}"
    )
    public void runExpirationCheck() {

        log.info(
                "Bắt đầu Scheduler xử lý Subscription hết hạn."
        );

        try {
            expirationService.processExpiredSubscriptions();

            log.info(
                    "Scheduler xử lý Subscription hết hạn đã kết thúc."
            );

        } catch (Exception exception) {
            log.error(
                    "Scheduler xử lý Subscription hết hạn gặp lỗi.",
                    exception
            );
        }
    }
}