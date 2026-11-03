package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.expiration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpirePendingReservationsScheduler {

    private final ExpirePendingReservationsService expirationService;

    /**
     * Kiểm tra và expire Reservation hết hạn.
     *
     * Mặc định chạy mỗi phút.
     */
    @Scheduled(
            cron = "${reservation.expiration-check.cron:0 * * * * *}",
            zone = "${reservation.expiration-check.zone:Asia/Ho_Chi_Minh}"
    )
    public void runExpirationCheck() {

        log.info(
                "Bắt đầu kiểm tra Reservation hết hạn."
        );

        try {

            int expiredCount =
                    expirationService.expirePendingReservations();

            log.info(
                    "Hoàn tất kiểm tra Reservation hết hạn. "
                            + "Số Reservation được expire: {}",
                    expiredCount
            );

        } catch (Exception exception) {

            log.error(
                    "Scheduler xử lý Reservation hết hạn gặp lỗi.",
                    exception
            );
        }
    }
}