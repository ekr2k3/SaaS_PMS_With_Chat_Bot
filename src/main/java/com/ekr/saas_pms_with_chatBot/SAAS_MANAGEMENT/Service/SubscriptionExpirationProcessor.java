package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPayment;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentStatus;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionPaymentRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionPlanRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubscriptionExpirationProcessor {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final TenantDeactivationService tenantDeactivationService;

    /**
     * Xử lý một Subscription trong một transaction riêng.
     *
     * Hỗ trợ:
     * 1. Subscription ACTIVE đến hạn.
     * 2. Subscription EXPIRED có Payment gia hạn sớm hợp lệ
     *    nhưng webhook xác nhận thành công sau khi hết hạn.
     */
    @Transactional
    public SubscriptionExpirationResult processOne(
            Long subscriptionId,
            LocalDate today
    ) {

        Subscription subscription = subscriptionRepository
                .findByIdForUpdate(subscriptionId)
                .orElse(null);

        if (subscription == null
                || subscription.getEndDate() == null) {
            return notExpired(subscriptionId);
        }

        Subscription.SubscriptionStatus currentStatus =
                subscription.getStatus();

        boolean wasExpired =
                currentStatus == Subscription.SubscriptionStatus.EXPIRED;

        boolean isActive =
                currentStatus == Subscription.SubscriptionStatus.ACTIVE;

        // Chỉ xử lý Subscription ACTIVE hoặc EXPIRED.
        if (!isActive && !wasExpired) {
            return notExpired(subscriptionId);
        }

        // Chưa đến hạn thì chưa áp dụng kỳ tiếp theo.
        if (!subscription.getEndDate().isBefore(today)) {
            return notExpired(subscriptionId);
        }

        // Kỳ tiếp theo bắt đầu vào ngày sau endDate hiện tại.
        LocalDate nextPeriodStart =
                subscription.getEndDate().plusDays(1);

        /*
         * Tìm tất cả Payment SUCCEEDED thuộc kỳ tiếp theo.
         *
         * Không lọc theo periodEnd tại câu truy vấn vì Plan B
         * có thể có thời hạn khác Plan A.
         */
        List<SubscriptionPayment> successfulPayments =
                paymentRepository.findSuccessfulPaymentsForPeriodStart(
                        subscriptionId,
                        PaymentStatus.SUCCEEDED,
                        nextPeriodStart
                );

        SubscriptionPayment validPayment = null;
        SubscriptionPlan nextPlan = null;

        for (SubscriptionPayment payment : successfulPayments) {

            Map<String, Object> metadata = payment.getMetadata();

            // Mặc định sử dụng Plan hiện tại.
            SubscriptionPlan candidatePlan = subscription.getPlan();

            /*
             * Nếu Payment thuộc luồng thanh toán sớm,
             * sử dụng Plan mà Owner đã chọn.
             */
            if (metadata != null
                    && metadata.containsKey("earlyRenewalPlanId")) {

                Object planIdValue =
                        metadata.get("earlyRenewalPlanId");

                final Long planId;

                try {
                    if (planIdValue == null) {
                        continue;
                    }

                    planId = Long.valueOf(planIdValue.toString());

                } catch (NumberFormatException exception) {
                    continue;
                }

                candidatePlan = planRepository.findById(planId)
                        .orElse(null);
            }

            // Bỏ qua Payment nếu Plan hoặc kỳ dịch vụ không hợp lệ.
            if (candidatePlan == null
                    || candidatePlan.getDuration() == null
                    || candidatePlan.getDuration() <= 0
                    || candidatePlan.getDurationUnit() == null
                    || payment.getPeriodStart() == null
                    || payment.getPeriodEnd() == null) {

                continue;
            }

            LocalDate expectedEndDate = calculateEndDate(
                    nextPeriodStart,
                    candidatePlan
            );

            /*
             * Chỉ chấp nhận Payment có:
             * - Trạng thái SUCCEEDED.
             * - Kỳ bắt đầu đúng ngày kế tiếp.
             * - Kỳ kết thúc đúng theo thời hạn Plan.
             */
            if (payment.getPeriodStart().equals(nextPeriodStart)
                    && payment.getPeriodEnd().equals(expectedEndDate)) {

                validPayment = payment;
                nextPlan = candidatePlan;
                break;
            }
        }

        /*
         * Trường hợp có Payment thành công hợp lệ:
         * áp dụng Plan và kỳ dịch vụ mới.
         */
        if (validPayment != null) {

            subscription.setPlan(nextPlan);
            subscription.setStartDate(
                    validPayment.getPeriodStart()
            );
            subscription.setEndDate(
                    validPayment.getPeriodEnd()
            );
            subscription.setStatus(
                    Subscription.SubscriptionStatus.ACTIVE
            );

            subscriptionRepository.save(subscription);

            /*
             * Chỉ phục hồi Tenant INACTIVE nếu Subscription
             * đã EXPIRED do hết hạn.
             *
             * Không tự động mở lại Tenant SUSPENDED.
             */
            if (wasExpired) {

                Tenant tenant = subscription.getTenant();

                if (tenant != null
                        && tenant.getStatus()
                        == Tenant.TenantStatus.INACTIVE) {

                    tenant.setStatus(Tenant.TenantStatus.ACTIVE);
                }
            }

            return notExpired(subscriptionId);
        }

        /*
         * Không có Payment hợp lệ.
         *
         * Nếu Subscription đã EXPIRED thì không xử lý lại,
         * tránh gửi email hết hạn nhiều lần.
         */
        if (wasExpired) {
            return notExpired(subscriptionId);
        }

        /*
         * Subscription vẫn ACTIVE nhưng không có Payment
         * hợp lệ cho kỳ tiếp theo: chuyển sang EXPIRED.
         */
        subscription.setStatus(
                Subscription.SubscriptionStatus.EXPIRED
        );

        subscriptionRepository.save(subscription);

        Tenant tenant = subscription.getTenant();

        boolean tenantDeactivated =
                tenantDeactivationService.deactivateTenant(
                        subscription
                );

        // Chỉ gửi email khi Tenant thực sự chuyển INACTIVE.
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

    /**
     * Tính ngày kết thúc theo thời hạn của Plan.
     */
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

    /**
     * Kết quả không phát sinh việc hết hạn mới.
     */
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