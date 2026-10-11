package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;


import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalService {

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final SubscriptionRepository subscriptionRepository;

    private final MailService mailService;

    /**
     * Entry point được Scheduler gọi.
     */
    public void sendRenewalReminders() {
        sendRenewalReminders(
                LocalDate.now(BUSINESS_ZONE)
        );
    }

    /**
     * Xử lý nhắc gia hạn tại một ngày xác định.
     *
     * Tham số today được tách riêng để có thể kiểm thử
     * các mốc ngày mà không phải chờ Scheduler chạy thật.
     */
    public void sendRenewalReminders(LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException(
                    "today must not be null"
            );
        }

        YearMonth nextMonth = YearMonth.from(today).plusMonths(1);

        LocalDate startOfNextMonth =
                nextMonth.atDay(1);

        LocalDate startOfMonthAfterNext =
                nextMonth.plusMonths(1).atDay(1);

        List<Subscription> subscriptions;

        // EX-06: Lỗi truy vấn DB không được làm Scheduler
        // liên tục ném exception ra ngoài.
        try {
            subscriptions =
                    subscriptionRepository.findRenewalCandidates(
                            Subscription.SubscriptionStatus.ACTIVE,
                            today,
                            startOfNextMonth,
                            startOfMonthAfterNext
                    );
        } catch (Exception exception) {
            log.error(
                    "Không thể truy vấn Subscription cần nhắc gia hạn. "
                            + "today={}, nextMonth={}",
                    today,
                    nextMonth,
                    exception
            );
            return;
        }

        if (subscriptions == null || subscriptions.isEmpty()) {
            log.info(
                    "Không có Subscription cần nhắc gia hạn. "
                            + "today={}, nextMonth={}",
                    today,
                    nextMonth
            );
            return;
        }

        log.info(
                "Bắt đầu gửi email nhắc gia hạn. "
                        + "today={}, nextMonth={}, total={}",
                today,
                nextMonth,
                subscriptions.size()
        );

        int sentCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (Subscription subscription : subscriptions) {
            try {
                ReminderResult result =
                        sendReminderForOneSubscription(
                                subscription
                        );

                switch (result) {
                    case SENT -> sentCount++;
                    case SKIPPED -> skippedCount++;
                    case FAILED -> failedCount++;
                }
            } catch (Exception exception) {
                // Phòng vệ cuối cùng: lỗi bất ngờ ở một bản ghi
                // không làm dừng vòng lặp.
                failedCount++;

                log.error(
                        "Lỗi không dự kiến khi xử lý Subscription ID={}",
                        subscription != null
                                ? subscription.getSubscriptionId()
                                : null,
                        exception
                );
            }
        }

        log.info(
                "Hoàn tất gửi email nhắc gia hạn. "
                        + "total={}, sent={}, skipped={}, failed={}",
                subscriptions.size(),
                sentCount,
                skippedCount,
                failedCount
        );
    }

    private ReminderResult sendReminderForOneSubscription(
            Subscription subscription
    ) {
        if (subscription == null
                || subscription.getSubscriptionId() == null) {
            log.warn(
                    "Bỏ qua bản ghi Subscription không hợp lệ."
            );
            return ReminderResult.SKIPPED;
        }

        Long subscriptionId =
                subscription.getSubscriptionId();

        // Kiểm tra quan hệ người nhận.
        Tenant tenant = subscription.getTenant();

        if (tenant == null) {
            log.warn(
                    "Bỏ qua Subscription ID={} vì không có Tenant.",
                    subscriptionId
            );
            return ReminderResult.SKIPPED;
        }

        Account account = tenant.getAccount();

        if (account == null) {
            log.warn(
                    "Bỏ qua Subscription ID={} vì Tenant ID={} "
                            + "không có Account.",
                    subscriptionId,
                    tenant.getTenantId()
            );
            return ReminderResult.SKIPPED;
        }

        String ownerEmail = account.getEmail();

        if (!isValidEmail(ownerEmail)) {
            log.warn(
                    "Bỏ qua Subscription ID={} vì email Owner "
                            + "không hợp lệ hoặc bị bỏ trống.",
                    subscriptionId
            );
            return ReminderResult.SKIPPED;
        }

        String ownerName = account.getName();

        if (ownerName == null || ownerName.isBlank()) {
            ownerName = "Quý khách";
        }

        String tenantName = tenant.getName();

        if (tenantName == null || tenantName.isBlank()) {
            tenantName = "Tổ chức của bạn";
        }

        LocalDate endDate = subscription.getEndDate();

        if (endDate == null) {
            log.warn(
                    "Bỏ qua Subscription ID={} vì endDate bị trống.",
                    subscriptionId
            );
            return ReminderResult.SKIPPED;
        }

        try {
            mailService.sendSubscriptionRenewalReminder(
                    ownerEmail,
                    ownerName,
                    tenantName,
                    subscriptionId,
                    endDate
            );

            // SENT: MailService/SMTP đã chấp nhận yêu cầu gửi.
            // Không có bảng lịch sử nên kết quả chỉ tồn tại
            // trong log và bộ nhớ của lần chạy này.
            log.info(
                    "SENT: Email nhắc gia hạn được chấp nhận gửi. "
                            + "subscriptionId={}, recipient={}, endDate={}",
                    subscriptionId,
                    ownerEmail,
                    endDate
            );

            return ReminderResult.SENT;

        } catch (Exception exception) {
            log.error(
                    "FAILED: Không gửi được email nhắc gia hạn. "
                            + "subscriptionId={}, recipient={}, endDate={}",
                    subscriptionId,
                    ownerEmail,
                    endDate,
                    exception
            );

            return ReminderResult.FAILED;
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        try {
            InternetAddress address =
                    new InternetAddress(email.trim());

            address.validate();

            // Chấp nhận đúng một địa chỉ email.
            return address.getAddress() != null
                    && address.getAddress().equals(email.trim());

        } catch (AddressException exception) {
            return false;
        }
    }

    private enum ReminderResult {
        SENT,
        SKIPPED,
        FAILED
    }
}
