package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES.EarlySubscriptionRenewalResponse;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPayment;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentGateway;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentStatus;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionPaymentRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionPlanRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import com.ekr.saas_pms_with_chatBot.config.StripeProperties;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EarlySubscriptionRenewalService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final StripeService stripeService;
    private final StripeProperties stripeProperties;

    @Transactional
    public EarlySubscriptionRenewalResponse createPayment(
            Long planId,
            Authentication authentication
    ) {
        // 1. Xác định Owner từ tài khoản đăng nhập.
        if (authentication == null
                || !StringUtils.hasText(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Không xác định được Owner"
            );
        }

        Account owner = accountRepository.findByEmail(
                authentication.getName()
        );

        if (owner == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Không tìm thấy Account"
            );
        }

        // 2. Lấy Tenant và Subscription của Owner.
        Tenant tenant = tenantRepository.findByAccount(owner)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account chưa có Tenant"
                ));

        Subscription found = subscriptionRepository.findByTenant(tenant)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Subscription"
                ));

        // Khóa Subscription để giảm nguy cơ tạo hai Payment
        // đồng thời cho cùng một kỳ.
        Subscription subscription = subscriptionRepository
                .findByIdForUpdate(found.getSubscriptionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Subscription"
                ));

        // 3. Chỉ cho thanh toán sớm khi Subscription còn ACTIVE.
        if (subscription.getStatus()
                != Subscription.SubscriptionStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ Subscription ACTIVE mới được thanh toán sớm"
            );
        }

        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tenant không ở trạng thái ACTIVE"
            );
        }

        if (subscription.getEndDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subscription chưa có ngày kết thúc"
            );
        }

        // 4. Xác minh Plan được chọn.
        SubscriptionPlan selectedPlan = planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Plan"
                ));

        if (selectedPlan.getStatus() != SubscriptionPlan.PlanStatus.ACTIVE
                || selectedPlan.getPrice() == null
                || selectedPlan.getPrice().signum() <= 0
                || selectedPlan.getDuration() == null
                || selectedPlan.getDuration() <= 0
                || selectedPlan.getDurationUnit() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Plan không hợp lệ hoặc không hoạt động"
            );
        }

        // 5. Tính kỳ tiếp theo từ ngày hết hạn hiện tại.
        LocalDate periodStart =
                subscription.getEndDate().plusDays(1);

        LocalDate periodEnd = calculateEndDate(
                periodStart,
                selectedPlan
        );

        // 6. Không tạo Payment mới nếu kỳ này đã được thanh toán.
        List<SubscriptionPayment> successfulPayments =
                paymentRepository.findSuccessfulPaymentsForPeriodStart(
                        subscription.getSubscriptionId(),
                        PaymentStatus.SUCCEEDED,
                        periodStart
                );

        if (!successfulPayments.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Kỳ tiếp theo đã có Payment thành công"
            );
        }

        // 7. Nếu đã có Checkout đang chờ cho kỳ này,
        // chỉ tái sử dụng khi đúng Plan và đúng khoảng thời gian.
        List<SubscriptionPayment> payments =
                paymentRepository.findPaymentsForPeriodStart(
                        subscription.getSubscriptionId(),
                        periodStart
                );

        for (SubscriptionPayment existing : payments) {
            if (existing.getStatus() != PaymentStatus.PENDING
                    || existing.getPaymentGateway()
                    != PaymentGateway.STRIPE) {
                continue;
            }

            Map<String, Object> metadata = existing.getMetadata();

            if (metadata == null
                    || !metadata.containsKey("earlyRenewalPlanId")) {
                continue;
            }

            String existingPlanId = String.valueOf(
                    metadata.get("earlyRenewalPlanId")
            );

            if (!String.valueOf(planId).equals(existingPlanId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Đã có Payment PENDING cho Plan khác. "
                                + "Hãy hoàn tất hoặc đợi Checkout cũ hết hạn."
                );
            }

            Object checkoutUrl = metadata.get("checkoutUrl");

            if (checkoutUrl != null
                    && StringUtils.hasText(checkoutUrl.toString())) {
                return toResponse(existing, checkoutUrl.toString());
            }

            // Payment đã được lưu nhưng Checkout URL chưa được lưu.
            // Tiếp tục khởi tạo lại Checkout bằng requestId cũ.
            return createCheckout(
                    existing,
                    selectedPlan,
                    subscription,
                    periodStart,
                    periodEnd
            );
        }

        // 8. Tạo Payment mới, chưa cập nhật Subscription.
        String currency = stripeProperties.getCurrency()
                .toUpperCase(java.util.Locale.ROOT);

        long stripeAmount = toStripeAmount(
                selectedPlan.getPrice(),
                currency
        );

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setSubscription(subscription);
        payment.setPaymentGateway(PaymentGateway.STRIPE);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(selectedPlan.getPrice());
        payment.setCurrency(currency);
        payment.setOrderId("SUB-EARLY-RENEW-" + UUID.randomUUID());
        payment.setRequestId("stripe-early-" + UUID.randomUUID());

        // Kỳ dịch vụ đã được xác định ngay khi tạo Payment.
        payment.setPeriodStart(periodStart);
        payment.setPeriodEnd(periodEnd);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("earlyRenewalPlanId", selectedPlan.getPlanId());
        metadata.put("earlyRenewalPlanName", selectedPlan.getName());

        payment.setMetadata(metadata);

        payment = paymentRepository.saveAndFlush(payment);

        // 9. Tạo Checkout Session.
        return createCheckout(
                payment,
                selectedPlan,
                subscription,
                periodStart,
                periodEnd
        );
    }

    private EarlySubscriptionRenewalResponse createCheckout(
            SubscriptionPayment payment,
            SubscriptionPlan selectedPlan,
            Subscription subscription,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        String currency = payment.getCurrency();

        long stripeAmount = toStripeAmount(
                payment.getAmount(),
                currency
        );

        try {
            Session session = stripeService.createCheckoutSession(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getRequestId(),
                    selectedPlan.getName(),
                    stripeAmount,
                    currency,
                    subscription.getSubscriptionId(),
                    selectedPlan.getPlanId()
            );

            if (!StringUtils.hasText(session.getId())
                    || !StringUtils.hasText(session.getUrl())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Stripe không trả về Checkout URL hợp lệ"
                );
            }

            Map<String, Object> metadata = payment.getMetadata() == null
                    ? new HashMap<>()
                    : new HashMap<>(payment.getMetadata());

            metadata.put("checkoutSessionId", session.getId());
            metadata.put("checkoutUrl", session.getUrl());
            metadata.put("earlyRenewalPlanId", selectedPlan.getPlanId());
            metadata.put("earlyRenewalPlanName", selectedPlan.getName());

            payment.setMetadata(metadata);
            payment.setPeriodStart(periodStart);
            payment.setPeriodEnd(periodEnd);

            paymentRepository.saveAndFlush(payment);

            return toResponse(payment, session.getUrl());

        } catch (StripeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể khởi tạo Stripe Checkout. Có thể thử lại.",
                    exception
            );
        }
    }

    private EarlySubscriptionRenewalResponse toResponse(
            SubscriptionPayment payment,
            String paymentUrl
    ) {
        return new EarlySubscriptionRenewalResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getStatus(),
                paymentUrl,
                payment.getAmount(),
                payment.getCurrency()
        );
    }

    private LocalDate calculateEndDate(
            LocalDate startDate,
            SubscriptionPlan plan
    ) {
        return switch (plan.getDurationUnit()) {
            case DAY -> startDate.plusDays(plan.getDuration());
            case MONTH -> startDate.plusMonths(plan.getDuration());
            case YEAR -> startDate.plusYears(plan.getDuration());
        };
    }

    private long toStripeAmount(
            BigDecimal amount,
            String currency
    ) {
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Số tiền không hợp lệ"
            );
        }

        if ("VND".equalsIgnoreCase(currency)
                || "JPY".equalsIgnoreCase(currency)
                || "KRW".equalsIgnoreCase(currency)) {
            return amount
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();
        }

        return amount
                .movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }
}
