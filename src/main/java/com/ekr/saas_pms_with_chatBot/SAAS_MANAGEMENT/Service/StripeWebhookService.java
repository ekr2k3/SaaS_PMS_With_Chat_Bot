package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPayment;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentGateway;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentStatus;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionPaymentRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionPlanRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import com.ekr.saas_pms_with_chatBot.config.StripeProperties;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class StripeWebhookService {

    private final StripeProperties stripeProperties;
    private final SubscriptionPaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;

    public StripeWebhookService(
            StripeProperties stripeProperties,
            SubscriptionPaymentRepository paymentRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository
    ) {
        this.stripeProperties = stripeProperties;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }


    private boolean isLateRenewal(SubscriptionPayment payment) {
        Map<String, Object> metadata = payment.getMetadata();

        return metadata != null
                && metadata.containsKey("renewalPlanId");
    }


    private Long parsePlanId(Object value, String fieldName) {
        if (value == null || !StringUtils.hasText(value.toString())) {
            throw new IllegalArgumentException(
                    "Thiếu metadata: " + fieldName
            );
        }

        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Metadata không hợp lệ: " + fieldName,
                    exception
            );
        }
    }


    /**
     * Xác minh chữ ký, phân loại Event và xử lý nghiệp vụ.
     *
     * Không lưu Event vào database.
     */
    @Transactional
    public void handleWebhook(
            String payload,
            String signature
    ) throws SignatureVerificationException {

        if (!StringUtils.hasText(stripeProperties.getWebhookSecret())) {
            throw new IllegalStateException(
                    "Stripe webhook secret chưa được cấu hình"
            );
        }

        if (!StringUtils.hasText(signature)) {
            throw new IllegalArgumentException(
                    "Thiếu Stripe-Signature"
            );
        }

        Event event = Webhook.constructEvent(
                payload,
                signature,
                stripeProperties.getWebhookSecret()
        );

        String eventType = event.getType();

        switch (eventType) {

            case "checkout.session.completed" -> {
                Session session = extractSession(event);

                // Với phương thức thanh toán bất đồng bộ,
                // completed chưa chắc đã có tiền.
                if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
                    handlePaymentSucceeded(session);
                }
            }

            case "checkout.session.async_payment_succeeded" -> {
                Session session = extractSession(event);
                handlePaymentSucceeded(session);
            }

            case "checkout.session.async_payment_failed" -> {
                Session session = extractSession(event);
                handlePaymentFailed(session);
            }

            case "checkout.session.expired" -> {
                Session session = extractSession(event);
                handleCheckoutExpired(session);
            }

            default -> {
                // Các Event không thuộc phạm vi Subscription Payment
                // không cần xử lý.
            }
        }
    }

    /**
     * Chuyển Event data thành Checkout Session.
     */
    private Session extractSession(Event event) {

        Optional<StripeObject> stripeObject =
                event.getDataObjectDeserializer().getObject();

        if (stripeObject.isEmpty()
                || !(stripeObject.get() instanceof Session)) {
            throw new IllegalArgumentException(
                    "Không thể đọc Checkout Session từ Stripe Event: "
                            + event.getType()
            );
        }

        return (Session) stripeObject.get();
    }

    /**
     * Xử lý thanh toán thành công.
     */
    private void handlePaymentSucceeded(Session session) {

        // 1. Xác minh danh tính Payment và khóa bản ghi.
        SubscriptionPayment payment =
                findAndValidatePayment(session);

        // 2. Idempotency: không xử lý lại Payment đã thành công.
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }

        // 3. Chỉ tiếp tục nếu Stripe xác nhận đã thanh toán.
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            return;
        }

        // 4. Xác minh số tiền và currency.
        validateAmountAndCurrency(session, payment);

        // 5. Lấy Subscription hiện tại.
        Subscription subscription = payment.getSubscription();

        if (subscription == null) {
            throw new IllegalStateException(
                    "Payment không liên kết với Subscription"
            );
        }

        // 6. Xác minh PaymentIntent.
        String paymentIntentId = session.getPaymentIntent();

        if (!StringUtils.hasText(paymentIntentId)) {
            throw new IllegalStateException(
                    "Stripe Session không có PaymentIntent ID"
            );
        }

        // 7. Xác định Plan và kỳ dịch vụ.
        SubscriptionPlan selectedPlan;
        LocalDate startDate;
        LocalDate endDate;

        // Capture một lần để ngày bắt đầu và paidAt nhất quán.
        LocalDateTime paidAt = LocalDateTime.now(
                java.time.ZoneId.of("Asia/Ho_Chi_Minh")
        );

        if (isLateRenewal(payment)) {

            // 7a. Gia hạn muộn: lấy Plan đã chọn khi tạo Payment.
            Map<String, Object> metadata = payment.getMetadata();

            Long renewalPlanId = parsePlanId(
                    metadata.get("renewalPlanId"),
                    "renewalPlanId"
            );

            selectedPlan = planRepository.findById(renewalPlanId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Không tìm thấy Plan gia hạn: "
                                    + renewalPlanId
                    ));

            // 7b. Kiểm tra thời hạn Plan.
            if (selectedPlan.getDuration() == null
                    || selectedPlan.getDuration() <= 0
                    || selectedPlan.getDurationUnit() == null) {
                throw new IllegalStateException(
                        "Plan gia hạn không có thời hạn hợp lệ"
                );
            }

            // 7c. Chỉ bắt đầu kỳ mới khi thanh toán được xác nhận.
            startDate = paidAt.toLocalDate();

            endDate = calculateEndDate(
                    startDate,
                    selectedPlan
            );

        } else {

            // 7d. Payment thông thường:
            // giữ logic Plan và kỳ sử dụng hiện tại.
            selectedPlan = subscription.getPlan();

            if (selectedPlan == null
                    || selectedPlan.getDuration() == null
                    || selectedPlan.getDuration() <= 0
                    || selectedPlan.getDurationUnit() == null) {
                throw new IllegalStateException(
                        "Subscription Plan không có thời hạn hợp lệ"
                );
            }

            if (payment.getPeriodStart() != null
                    && payment.getPeriodEnd() != null) {

                // Payment đã được gắn với một kỳ cụ thể.
                startDate = payment.getPeriodStart();
                endDate = payment.getPeriodEnd();

            } else if (payment.getPeriodStart() == null
                    && payment.getPeriodEnd() == null
                    && (subscription.getStartDate() == null
                    || subscription.getEndDate() == null)) {

                // Thanh toán lần đầu theo logic hiện tại.
                startDate = paidAt.toLocalDate();

                endDate = calculateEndDate(
                        startDate,
                        selectedPlan
                );

            } else {
                throw new IllegalStateException(
                        "Payment thiếu hoặc không đầy đủ "
                                + "periodStart/periodEnd. paymentId="
                                + payment.getId()
                );
            }
        }

        // 8. Kiểm tra kỳ dịch vụ.
        if (!endDate.isAfter(startDate)) {
            throw new IllegalStateException(
                    "Kỳ thanh toán không hợp lệ. paymentId="
                            + payment.getId()
            );
        }

        // 9. Cập nhật Payment.
        payment.setTransactionId(paymentIntentId);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(paidAt);
        payment.setPeriodStart(startDate);
        payment.setPeriodEnd(endDate);

        paymentRepository.save(payment);

        // 10. Cập nhật Subscription.
        // Gia hạn muộn có thể chuyển từ Plan A sang Plan B.
        subscription.setPlan(selectedPlan);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setStatus(
                Subscription.SubscriptionStatus.ACTIVE
        );

        subscriptionRepository.save(subscription);

        // 11. Kích hoạt Tenant nếu đang INACTIVE.
        // Không tự động mở lại Tenant SUSPENDED.
        Tenant tenant = subscription.getTenant();

        if (tenant != null
                && tenant.getStatus() == Tenant.TenantStatus.INACTIVE) {
            tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        }

        // Tenant được cập nhật qua dirty checking trong transaction.
    }

    /**
     * Xử lý trường hợp Stripe báo thanh toán thất bại.
     */
    private void handlePaymentFailed(Session session) {

        SubscriptionPayment payment =
                findAndValidatePayment(session);

        // Không ghi đè giao dịch đã thành công.
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }

        // Không ghi đè các trạng thái thanh toán đã kết thúc.
        if (payment.getStatus() == PaymentStatus.CANCELLED
                || payment.getStatus() == PaymentStatus.REFUNDED
                || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED) {
            return;
        }

        // Chỉ chuyển trạng thái đang chờ xử lý sang FAILED.
        if (payment.getStatus() == PaymentStatus.PENDING
                || payment.getStatus() == PaymentStatus.PROCESSING) {

            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }

        // Không kích hoạt Subscription.
        // Không kích hoạt Tenant.
    }

    /**
     * Checkout Session hết hạn trước khi thanh toán.
     */
    private void handleCheckoutExpired(Session session) {

        SubscriptionPayment payment = findAndValidatePayment(session);

        // Không hủy một Payment đã thành công.
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }

        if (payment.getStatus() == PaymentStatus.PENDING
                || payment.getStatus() == PaymentStatus.PROCESSING) {

            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
        }
    }

    /**
     * Tìm Payment theo ID được Stripe gửi trong Session metadata,
     * khóa bản ghi và xác minh Session có đúng với Payment đó không.
     */
    private SubscriptionPayment findAndValidatePayment(
            Session session
    ) {
        // 1. Đọc metadata từ Stripe Checkout Session.
        Map<String, String> metadata = session.getMetadata();

        if (metadata == null) {
            throw new IllegalArgumentException(
                    "Stripe Session thiếu metadata"
            );
        }

        String paymentIdValue =
                metadata.get("subscription_payment_id");

        String orderId = metadata.get("order_id");

        String subscriptionIdValue =
                metadata.get("subscription_id");

        if (!StringUtils.hasText(paymentIdValue)
                || !StringUtils.hasText(orderId)
                || !StringUtils.hasText(subscriptionIdValue)) {
            throw new IllegalArgumentException(
                    "Stripe Session thiếu metadata định danh Payment"
            );
        }

        // 2. Parse các ID.
        final Long paymentId;
        final Long subscriptionId;

        try {
            paymentId = Long.valueOf(paymentIdValue);
            subscriptionId = Long.valueOf(subscriptionIdValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Metadata ID của Stripe Session không hợp lệ",
                    exception
            );
        }

        // 3. Tìm và khóa Payment.
        SubscriptionPayment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy SubscriptionPayment: " + paymentId
                ));

        // 4. Xác minh Payment thuộc Stripe.
        if (payment.getPaymentGateway() != PaymentGateway.STRIPE) {
            throw new IllegalArgumentException(
                    "Payment không thuộc Stripe"
            );
        }

        // 5. Xác minh Order ID.
        if (!orderId.equals(payment.getOrderId())) {
            throw new IllegalArgumentException(
                    "Order ID không khớp với Payment trong database"
            );
        }

        // 6. Xác minh Subscription ID.
        Subscription subscription = payment.getSubscription();

        if (subscription == null
                || !subscriptionId.equals(
                subscription.getSubscriptionId()
        )) {
            throw new IllegalArgumentException(
                    "Subscription ID không khớp với Payment"
            );
        }

        // 7. Xác minh Checkout Session ID.
        Map<String, Object> paymentMetadata = payment.getMetadata();

        if (paymentMetadata == null) {
            throw new IllegalArgumentException(
                    "Payment chưa lưu metadata"
            );
        }

        Object storedSessionId =
                paymentMetadata.get("checkoutSessionId");

        if (storedSessionId == null
                || !session.getId().equals(storedSessionId.toString())) {
            throw new IllegalArgumentException(
                    "Checkout Session ID không khớp với Payment"
            );
        }

        // 8. Chỉ bắt buộc đối chiếu Plan đối với gia hạn muộn.
        if (isLateRenewal(payment)) {
            Long storedPlanId = parsePlanId(
                    paymentMetadata.get("renewalPlanId"),
                    "renewalPlanId"
            );

            Long stripePlanId = parsePlanId(
                    metadata.get("plan_id"),
                    "plan_id"
            );

            if (!storedPlanId.equals(stripePlanId)) {
                throw new IllegalArgumentException(
                        "Plan ID trên Stripe không khớp với Payment"
                );
            }
        }

        return payment;
    }

    /**
     * So sánh số tiền và tiền tệ trong Session với Payment.
     */
    private void validateAmountAndCurrency(
            Session session,
            SubscriptionPayment payment
    ) {

        String paymentCurrency = payment.getCurrency();
        String sessionCurrency = session.getCurrency();

        if (!StringUtils.hasText(paymentCurrency)
                || !StringUtils.hasText(sessionCurrency)
                || !paymentCurrency.equalsIgnoreCase(sessionCurrency)) {
            throw new IllegalArgumentException(
                    "Currency của Stripe Session không khớp Payment"
            );
        }

        Long stripeAmount = session.getAmountTotal();

        if (stripeAmount == null) {
            throw new IllegalArgumentException(
                    "Stripe Session không có amount_total"
            );
        }

        long expectedAmount = toStripeAmount(
                payment.getAmount(),
                paymentCurrency
        );

        if (stripeAmount != expectedAmount) {
            throw new IllegalArgumentException(
                    "Số tiền Stripe không khớp với Payment"
            );
        }
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
     * Chuyển số tiền từ đơn vị lưu trong DB sang đơn vị nhỏ nhất
     * Stripe yêu cầu.
     *
     * VND, JPY, KRW được xử lý là tiền tệ không có phần thập phân.
     * Các currency còn lại trong implementation này dùng 2 chữ số.
     */
    private long toStripeAmount(
            BigDecimal amount,
            String currency
    ) {

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount không hợp lệ"
            );
        }

        String normalizedCurrency =
                currency.toUpperCase(Locale.ROOT);

        if ("VND".equals(normalizedCurrency)
                || "JPY".equals(normalizedCurrency)
                || "KRW".equals(normalizedCurrency)) {

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
