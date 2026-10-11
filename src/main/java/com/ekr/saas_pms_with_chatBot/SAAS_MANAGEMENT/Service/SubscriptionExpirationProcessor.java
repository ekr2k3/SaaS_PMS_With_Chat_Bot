package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPayment;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentStatus;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionPaymentRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionExpirationProcessor {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final TenantDeactivationService tenantDeactivationService;

    /**
     * Xử lý một Subscription trong một transaction riêng.
     */
    @Transactional
    public SubscriptionExpirationResult processOne(
            Long subscriptionId,
            LocalDate today
    ) {

        Subscription subscription = subscriptionRepository
                .findByIdForUpdate(subscriptionId)
                .orElse(null);

        // Subscription có thể đã bị một tiến trình khác xử lý.
        if (subscription == null
                || subscription.getStatus()
                != Subscription.SubscriptionStatus.ACTIVE
                || subscription.getEndDate() == null
                || !subscription.getEndDate().isBefore(today)) {

            return notExpired(subscriptionId);
        }

        SubscriptionPlan plan = subscription.getPlan();

        if (plan == null
                || plan.getDuration() == null
                || plan.getDuration() <= 0
                || plan.getDurationUnit() == null) {

            throw new IllegalStateException(
                    "Subscription Plan không có thời hạn hợp lệ. "
                            + "subscriptionId=" + subscriptionId
            );
        }

        // Kỳ tiếp theo bắt đầu vào ngày sau endDate hiện tại.
        LocalDate nextPeriodStart =
                subscription.getEndDate().plusDays(1);

        LocalDate nextPeriodEnd =
                calculateEndDate(nextPeriodStart, plan);

        List<SubscriptionPayment> successfulPayments =
                paymentRepository.findSuccessfulPaymentsForPeriod(
                        subscriptionId,
                        PaymentStatus.SUCCEEDED,
                        nextPeriodStart,
                        nextPeriodEnd
                );

        if (!successfulPayments.isEmpty()) {

            // Payment đã thành công đúng kỳ:
            // gia hạn bằng chính khoảng thời gian của Payment.
            subscription.setStartDate(nextPeriodStart);
            subscription.setEndDate(nextPeriodEnd);
            subscription.setStatus(
                    Subscription.SubscriptionStatus.ACTIVE
            );

            subscriptionRepository.save(subscription);

            return notExpired(subscriptionId);
        }

        // Không có Payment thành công cho đúng kỳ:
        // đánh dấu Subscription hết hạn.
        subscription.setStatus(
                Subscription.SubscriptionStatus.EXPIRED
        );

        subscriptionRepository.save(subscription);

        Tenant tenant = subscription.getTenant();

        boolean tenantDeactivated =
                tenantDeactivationService.deactivateTenant(subscription);

        // Chỉ yêu cầu gửi email khi tiến trình thực sự
        // chuyển Tenant sang INACTIVE.
        if (!tenantDeactivated) {
            return notExpired(subscriptionId);
        }

        return new SubscriptionExpirationResult(
                true,
                subscription.getSubscriptionId(),
                tenant.getAccount().getEmail(),
                tenant.getAccount().getName(),
                tenant.getName()
        );
    }

    private LocalDate calculateEndDate(
            LocalDate startDate,
            SubscriptionPlan plan
    ) {

        int duration = plan.getDuration();

        return switch (plan.getDurationUnit()) {
            case DAY -> startDate.plusDays(duration);
            case MONTH -> startDate.plusMonths(duration);
            case YEAR -> startDate.plusYears(duration);
        };
    }

    private SubscriptionExpirationResult notExpired(
            Long subscriptionId
    ) {
        return new SubscriptionExpirationResult(
                false,
                subscriptionId,
                null,
                null,
                null
        );
    }
}