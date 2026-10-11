package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPayment;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentGateway;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentStatus;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionPaymentRepository;
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

    public StripeWebhookService(
            StripeProperties stripeProperties,
            SubscriptionPaymentRepository paymentRepository,
            SubscriptionRepository subscriptionRepository
    ) {
        this.stripeProperties = stripeProperties;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
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

        // 1. Tìm và xác minh Payment từ Stripe Checkout Session.
        SubscriptionPayment payment = findAndValidatePayment(session);

        // 2. Idempotency: Payment đã thành công thì không xử lý lại.
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }

        // 3. Chỉ xử lý khi Stripe xác nhận đã thanh toán.
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            return;
        }

        // 4. Xác minh số tiền và tiền tệ.
        validateAmountAndCurrency(session, payment);

        // 5. Lấy Subscription liên kết với Payment.
        Subscription subscription = payment.getSubscription();

        if (subscription == null) {
            throw new IllegalStateException(
                    "Payment không liên kết với Subscription"
            );
        }

        // 6. Kiểm tra Plan.
        SubscriptionPlan plan = subscription.getPlan();

        if (plan == null
                || plan.getDuration() == null
                || plan.getDurationUnit() == null
                || plan.getDuration() <= 0) {

            throw new IllegalStateException(
                    "Subscription Plan không có thời hạn hợp lệ"
            );
        }

        // 7. Lấy PaymentIntent ID từ Stripe.
        String paymentIntentId = session.getPaymentIntent();

        if (!StringUtils.hasText(paymentIntentId)) {
            throw new IllegalStateException(
                    "Stripe Session không có PaymentIntent ID"
            );
        }

        // 8. Xác định kỳ dịch vụ mà Payment thanh toán.
        LocalDate startDate;
        LocalDate endDate;

        if (payment.getPeriodStart() != null
                && payment.getPeriodEnd() != null) {

            // Payment đã được gắn với một kỳ dịch vụ cụ thể.
            startDate = payment.getPeriodStart();
            endDate = payment.getPeriodEnd();

        } else if (payment.getPeriodStart() == null
                && payment.getPeriodEnd() == null
                && (subscription.getStartDate() == null
                || subscription.getEndDate() == null)) {

            // Thanh toán lần đầu: Subscription chưa có kỳ sử dụng.
            startDate = LocalDate.now(
                    java.time.ZoneId.of("Asia/Ho_Chi_Minh")
            );

            endDate = calculateEndDate(startDate, plan);

        } else {

            // Không tự ý ghi đè kỳ sử dụng đang có.
            throw new IllegalStateException(
                    "Payment thiếu hoặc không đầy đủ periodStart/periodEnd "
                            + "cho Subscription đã có kỳ sử dụng. paymentId="
                            + payment.getId()
            );
        }

        // 9. Kiểm tra kỳ dịch vụ hợp lệ.
        if (!endDate.isAfter(startDate)) {
            throw new IllegalStateException(
                    "Kỳ thanh toán không hợp lệ. paymentId="
                            + payment.getId()
            );
        }

        // 10. Cập nhật thông tin Payment.
        payment.setTransactionId(paymentIntentId);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(
                LocalDateTime.now(
                        java.time.ZoneId.of("Asia/Ho_Chi_Minh")
                )
        );

        paymentRepository.save(payment);

        // 11. Cập nhật kỳ sử dụng Subscription theo kỳ của Payment.
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setStatus(
                Subscription.SubscriptionStatus.ACTIVE
        );

        subscriptionRepository.save(subscription);

        // 12. Kích hoạt Tenant nếu đang INACTIVE.
        // Không tự động gỡ trạng thái SUSPENDED.
        Tenant tenant = subscription.getTenant();

        if (tenant != null
                && tenant.getStatus() == Tenant.TenantStatus.INACTIVE) {

            tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        }

        // Các thay đổi được ghi nhận trong transaction hiện tại.
    }

    /**
     * Xử lý trường hợp Stripe báo thanh toán thất bại.
     */
    private void handlePaymentFailed(Session session) {

        SubscriptionPayment payment = findAndValidatePayment(session);

        // Không được hạ trạng thái một giao dịch đã thành công.
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }

        if (payment.getStatus() == PaymentStatus.REFUNDED
                || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED) {
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
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

        SubscriptionPayment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy SubscriptionPayment: " + paymentId
                ));

        if (payment.getPaymentGateway() != PaymentGateway.STRIPE) {
            throw new IllegalArgumentException(
                    "Payment không thuộc Stripe"
            );
        }

        if (!orderId.equals(payment.getOrderId())) {
            throw new IllegalArgumentException(
                    "Order ID không khớp với Payment trong database"
            );
        }

        Subscription subscription = payment.getSubscription();

        if (subscription == null
                || !subscriptionId.equals(subscription.getSubscriptionId())) {
            throw new IllegalArgumentException(
                    "Subscription ID không khớp với Payment"
            );
        }

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
