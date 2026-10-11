package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES.LateSubscriptionRenewalResponse;
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
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LateSubscriptionRenewalService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final StripeService stripeService;
    private final StripeProperties stripeProperties;

    public LateSubscriptionRenewalResponse createRenewalPayment(
            Long planId,
            Authentication authentication
    ) {
        if (authentication == null
                || !StringUtils.hasText(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Không xác định được Owner"
            );
        }

        Account owner = accountRepository.findByEmail(authentication.getName());
        if (owner == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Không tìm thấy Account"
            );
        }

        Tenant tenant = tenantRepository.findByAccount(owner)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account chưa có Tenant"
                ));

        Subscription subscription = subscriptionRepository.findByTenant(tenant)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Subscription"
                ));

        if (subscription.getStatus() != Subscription.SubscriptionStatus.EXPIRED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ Subscription EXPIRED mới được dùng API gia hạn muộn"
            );
        }

        if (tenant.getStatus() != Tenant.TenantStatus.INACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tenant không ở trạng thái INACTIVE do hết hạn"
            );
        }

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Plan"
                ));

        if (plan.getStatus() != SubscriptionPlan.PlanStatus.ACTIVE
                || plan.getPrice() == null
                || plan.getPrice().signum() <= 0
                || plan.getDuration() == null
                || plan.getDuration() <= 0
                || plan.getDurationUnit() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Plan không hợp lệ hoặc không hoạt động"
            );
        }

        // Tái sử dụng Payment PENDING của cùng Subscription/Stripe nếu có.
        SubscriptionPayment payment = paymentRepository
                .findFirstBySubscription_SubscriptionIdAndPaymentGatewayOrderByIdDesc(
                        subscription.getSubscriptionId(),
                        PaymentGateway.STRIPE
                )
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .orElse(null);

        String currency = stripeProperties.getCurrency().toUpperCase();
        long stripeAmount = toStripeAmount(plan.getPrice(), currency);

        if (payment != null) {
            Map<String, Object> existingMetadata = payment.getMetadata();
            String existingPlanId = existingMetadata == null
                    ? null
                    : String.valueOf(existingMetadata.get("renewalPlanId"));

            // Không tái sử dụng Checkout cũ cho Plan khác.
            if (!String.valueOf(planId).equals(existingPlanId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Đang có Payment PENDING cho Plan khác. "
                                + "Hãy hoàn tất hoặc đợi Checkout cũ hết hạn."
                );
            }

            String existingUrl = existingMetadata == null
                    ? null
                    : (String) existingMetadata.get("checkoutUrl");

            if (StringUtils.hasText(existingUrl)) {
                return new LateSubscriptionRenewalResponse(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getStatus(),
                        existingUrl,
                        payment.getAmount(),
                        payment.getCurrency()
                );
            }
        } else {
            payment = new SubscriptionPayment();
            payment.setSubscription(subscription);
            payment.setPaymentGateway(PaymentGateway.STRIPE);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setAmount(plan.getPrice());
            payment.setCurrency(currency);
            payment.setOrderId("SUB-RENEW-" + UUID.randomUUID());
            payment.setRequestId("stripe-renew-" + UUID.randomUUID());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("renewalPlanId", plan.getPlanId());
            metadata.put("renewalPlanName", plan.getName());
            payment.setMetadata(metadata);

            // Lưu trước khi gọi Stripe để giữ orderId/requestId khi retry.
            payment = paymentRepository.saveAndFlush(payment);
        }

        try {
            Session session = stripeService.createCheckoutSession(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getRequestId(),
                    plan.getName(),
                    stripeAmount,
                    currency,
                    subscription.getSubscriptionId(),
                    plan.getPlanId()
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
            metadata.put("renewalPlanId", plan.getPlanId());
            metadata.put("renewalPlanName", plan.getName());

            payment.setMetadata(metadata);
            paymentRepository.saveAndFlush(payment);

            return new LateSubscriptionRenewalResponse(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getStatus(),
                    session.getUrl(),
                    payment.getAmount(),
                    payment.getCurrency()
            );

        } catch (StripeException exception) {
            // Giữ Payment PENDING và requestId để có thể retry cùng idempotency key.
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể khởi tạo Stripe Checkout. Có thể thử lại.",
                    exception
            );
        }
    }

    private long toStripeAmount(BigDecimal amount, String currency) {
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Số tiền không hợp lệ"
            );
        }

        if ("VND".equalsIgnoreCase(currency)
                || "JPY".equalsIgnoreCase(currency)
                || "KRW".equalsIgnoreCase(currency)) {
            return amount.setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();
        }

        return amount.movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }
}