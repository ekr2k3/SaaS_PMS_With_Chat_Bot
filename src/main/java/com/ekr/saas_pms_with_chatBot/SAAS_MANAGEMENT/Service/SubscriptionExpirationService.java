package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpirationService {

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionExpirationProcessor expirationProcessor;
    private final MailService mailService;

    /**
     * Entry point do SubscriptionExpirationScheduler gọi.
     */
    public void processExpiredSubscriptions() {
        processExpiredSubscriptions(
                LocalDate.now(BUSINESS_ZONE)
        );
    }

    /**
     * Tách tham số today để có thể kiểm thử mà không cần chờ lịch.
     */
    public void processExpiredSubscriptions(LocalDate today) {

        if (today == null) {
            throw new IllegalArgumentException(
                    "today must not be null"
            );
        }

        List<Subscription> candidates;

        try {
            candidates = subscriptionRepository.findExpiredCandidates(
                    Subscription.SubscriptionStatus.ACTIVE,
                    today
            );
        } catch (Exception exception) {
            log.error(
                    "Không thể truy vấn Subscription đến hạn. today={}",
                    today,
                    exception
            );

            // Không được coi lỗi DB là không có Payment.
            return;
        }

        if (candidates == null || candidates.isEmpty()) {
            log.info(
                    "Không có Subscription cần xử lý hết hạn. today={}",
                    today
            );
            return;
        }

        log.info(
                "Bắt đầu xử lý Subscription hết hạn. today={}, total={}",
                today,
                candidates.size()
        );

        int renewedOrSkipped = 0;
        int expired = 0;
        int failed = 0;

        for (Subscription candidate : candidates) {

            Long subscriptionId = candidate.getSubscriptionId();

            try {
                SubscriptionExpirationResult result =
                        expirationProcessor.processOne(
                                subscriptionId,
                                today
                        );

                if (!result.expired()) {
                    renewedOrSkipped++;
                    continue;
                }

                // Processor đã commit transaction trước khi trả về.
                // Lỗi email không được làm hoàn tác trạng thái DB.
                try {
                    mailService.sendSubscriptionExpiredEmail(
                            result.ownerEmail(),
                            result.ownerName(),
                            result.tenantName(),
                            result.subscriptionId()
                    );

                    log.info(
                            "Đã gửi email thông báo Subscription hết hạn. "
                                    + "subscriptionId={}, recipient={}",
                            result.subscriptionId(),
                            result.ownerEmail()
                    );

                } catch (Exception mailException) {
                    log.error(
                            "Không gửi được email hết hạn. "
                                    + "subscriptionId={}, recipient={}",
                            result.subscriptionId(),
                            result.ownerEmail(),
                            mailException
                    );
                }

                expired++;

            } catch (Exception exception) {
                failed++;

                log.error(
                        "Không thể xử lý Subscription hết hạn. "
                                + "subscriptionId={}",
                        subscriptionId,
                        exception
                );
            }
        }

        log.info(
                "Hoàn tất xử lý Subscription hết hạn. "
                        + "total={}, renewedOrSkipped={}, expired={}, failed={}",
                candidates.size(),
                renewedOrSkipped,
                expired,
                failed
        );
    }
}