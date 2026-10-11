package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalScheduler {

    private final SubscriptionRenewalService renewalService;

    /**
     * Chạy lúc 09:00 ngày 28 hằng tháng.
     *
     * Cron và timezone được đọc từ application.properties.
     */
    @Scheduled(
            cron = "${subscription.renewal-reminder.cron:0 0 9 28 * *}",
            zone = "${subscription.renewal-reminder.zone:Asia/Ho_Chi_Minh}"
    )
    public void runRenewalReminder() {
        log.info("Bắt đầu Scheduler nhắc gia hạn Subscription.");

        try {
            renewalService.sendRenewalReminders();

            log.info(
                    "Scheduler nhắc gia hạn Subscription đã kết thúc."
            );
        } catch (Exception exception) {
            // Ngăn exception bất ngờ thoát khỏi scheduled method.
            log.error(
                    "Scheduler nhắc gia hạn Subscription gặp lỗi.",
                    exception
            );
        }
    }
}
