
package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPayment;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentGateway;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentStatus;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionPaymentRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import com.ekr.saas_pms_with_chatBot.config.StripeProperties;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminSubscriptionPaymentService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final StripeService stripeService;
    private final StripeProperties stripeProperties;
    private final MailService mailService;

    public AdminSubscriptionPaymentService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionPaymentRepository paymentRepository,
            StripeService stripeService,
            StripeProperties stripeProperties,
            MailService mailService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.stripeService = stripeService;
        this.stripeProperties = stripeProperties;
        this.mailService = mailService;
    }

    @Transactional
    public void approveAndCreatePayment(Long subscriptionId) {

        // 1. Tìm Subscription
        Subscription subscription = subscriptionRepository
                .findById(subscriptionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Subscription"
                ));

        // 2. Kiểm tra trạng thái.
        // PENDING được hỗ trợ tạm thời cho dữ liệu cũ.
        Subscription.SubscriptionStatus currentStatus =
                subscription.getStatus();

        boolean waitingForApproval =
                currentStatus == Subscription.SubscriptionStatus.PENDING_APPROVAL
                        || currentStatus == Subscription.SubscriptionStatus.PENDING;

        if (!waitingForApproval
                && currentStatus != Subscription.SubscriptionStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subscription không ở trạng thái có thể khởi tạo thanh toán"
            );
        }

        // 3. Kiểm tra Plan, Tenant, Owner
        SubscriptionPlan plan = subscription.getPlan();
        Tenant tenant = subscription.getTenant();

        if (plan == null || tenant == null || tenant.getAccount() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subscription thiếu Plan, Tenant hoặc Owner"
            );
        }

        if (plan.getStatus() != SubscriptionPlan.PlanStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subscription Plan hiện không hoạt động"
            );
        }

        Account owner = tenant.getAccount();

        if (owner.getEmail() == null || owner.getEmail().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Owner chưa có email hợp lệ"
            );
        }

        BigDecimal amount = plan.getPrice();

        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Giá Subscription phải lớn hơn 0"
            );
        }

        String currency =
                stripeProperties.getCurrency().toUpperCase();

        long stripeAmount = toStripeAmount(amount, currency);

        // 4. Tìm payment hiện có của Subscription.
        // Không tạo bản ghi mới nếu đây là lần retry.
        SubscriptionPayment payment = paymentRepository
                .findFirstBySubscription_SubscriptionIdAndPaymentGatewayOrderByIdDesc(
                        subscriptionId,
                        PaymentGateway.STRIPE
                )
                .orElse(null);

        if (payment != null) {

            // Đã có URL: dùng lại và gửi lại email.
            String existingUrl = getMetadataString(
                    payment,
                    "checkoutUrl"
            );

            if (existingUrl != null && !existingUrl.isBlank()) {
                mailService.sendPaymentEmail(
                        owner.getEmail(),
                        existingUrl,
                        payment.getAmount(),
                        payment.getCurrency(),
                        plan.getName()
                );

                subscription.setStatus(
                        Subscription.SubscriptionStatus.PENDING_PAYMENT
                );
                subscriptionRepository.save(subscription);
                return;
            }

            // Nếu đã tồn tại payment nhưng không thể retry an toàn,
            // không âm thầm tạo thêm payment mới.
            if (payment.getStatus() != PaymentStatus.PENDING) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Payment hiện tại không còn ở trạng thái PENDING"
                );
            }

            // Không thay requestId/orderId khi retry.
        } else {

            // 5. Đánh dấu Subscription đã được duyệt
            subscription.setStatus(
                    Subscription.SubscriptionStatus.PENDING_PAYMENT
            );
            subscriptionRepository.save(subscription);

            // 6. Tạo Payment record trước khi gọi Stripe
            payment = new SubscriptionPayment();

            payment.setSubscription(subscription);
            payment.setPaymentGateway(PaymentGateway.STRIPE);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setAmount(amount);
            payment.setCurrency(currency);

            payment.setOrderId("SUB-" + UUID.randomUUID());
            payment.setRequestId("stripe-sub-" + UUID.randomUUID());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("planId", plan.getPlanId());
            metadata.put("planName", plan.getName());
            metadata.put("ownerEmail", owner.getEmail());

            payment.setMetadata(metadata);



            payment = paymentRepository.saveAndFlush(payment);
        }

        // 7. Nếu đã có URL thì đã xử lý ở trên.
        // Nếu chưa có URL, gọi Stripe bằng requestId đã lưu.
        try {
            Session session = stripeService.createCheckoutSession(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getRequestId(),
                    plan.getName(),
                    stripeAmount,
                    currency,
                    subscriptionId,
                    plan.getPlanId()
            );

            if (session.getId() == null
                    || session.getUrl() == null
                    || session.getUrl().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Stripe không trả về Checkout Session URL hợp lệ"
                );
            }

            // 8. Lưu Checkout Session ID và URL.
            // Không ghi session.getId() vào transactionId.
            Map<String, Object> metadata = payment.getMetadata();

            if (metadata == null) {
                metadata = new HashMap<>();
            } else {
                metadata = new HashMap<>(metadata);
            }

            metadata.put("checkoutSessionId", session.getId());
            metadata.put("checkoutUrl", session.getUrl());

            payment.setMetadata(metadata);
            paymentRepository.saveAndFlush(payment);

            // 9. Gửi email cho Owner
            mailService.sendPaymentEmail(
                    owner.getEmail(),
                    session.getUrl(),
                    amount,
                    currency,
                    plan.getName()
            );

        } catch (StripeException e) {
            // Giữ payment PENDING và requestId để retry cùng idempotency key.
            // Không tạo payment mới chỉ vì lần gọi Stripe bị lỗi.
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể tạo hoặc xác nhận Stripe Checkout Session. "
                            + "Hãy retry bằng cùng payment record.",
                    e
            );
        }
    }

    private String getMetadataString(
            SubscriptionPayment payment,
            String key
    ) {
        Map<String, Object> metadata = payment.getMetadata();

        if (metadata == null) {
            return null;
        }

        Object value = metadata.get(key);
        return value == null ? null : value.toString();
    }

    private long toStripeAmount(
            BigDecimal amount,
            String currency
    ) {
        // VND là đồng tiền không dùng phần thập phân khi gửi Stripe.
        if ("VND".equals(currency)
                || "JPY".equals(currency)
                || "KRW".equals(currency)) {

            return amount
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();
        }

        // Ví dụ USD: 10.00 USD -> 1000 cents.
        return amount
                .movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }
}