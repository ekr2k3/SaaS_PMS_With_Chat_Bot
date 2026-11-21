
package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.webhook;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationPaymentRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.confirmation.ReservationConfirmedEvent;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ReservationStripeWebhookService {

    private final ReservationPaymentRepository paymentRepository;

    private final ReservationRepository reservationRepository;

    private final ReservationStripePaymentIntentGateway paymentIntentGateway;

    private final ApplicationEventPublisher eventPublisher;

    public ReservationStripeWebhookService(
            ReservationPaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            ReservationStripePaymentIntentGateway paymentIntentGateway,
            ApplicationEventPublisher eventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.paymentIntentGateway = paymentIntentGateway;
        this.eventPublisher = eventPublisher;
    }

    // =========================================================
    // 1. PUBLIC WEBHOOK ENTRY POINTS
    // =========================================================

    /**
     * Xử lý Reservation Webhook được xác minh bởi StripeWebhookRouterService
     * của luồng thanh toán SaaS cũ.
     *
     * PaymentIntent sẽ được truy vấn bằng SaaS Stripe Secret Key.
     *
     * Không thay đổi endpoint Webhook SaaS hiện tại.
     */
    @Transactional
    public void handleVerifiedEvent(Event event) {

        handleEvent(event, null, null);
    }

    /**
     * Xử lý Reservation Webhook được xác minh bởi Property Webhook.
     *
     * @param event             Stripe Event đã xác minh chữ ký
     * @param expectedPropertyId Property đã xác minh Webhook
     * @param propertySecretKey Stripe Secret Key của Property
     */
    @Transactional
    public void handleVerifiedPropertyEvent(
            Event event,
            Long expectedPropertyId,
            String propertySecretKey
    ) {

        if (expectedPropertyId == null) {
            throw new IllegalArgumentException(
                    "Property ID không hợp lệ"
            );
        }

        if (!StringUtils.hasText(propertySecretKey)) {
            throw new IllegalArgumentException(
                    "Stripe Property Secret Key không hợp lệ"
            );
        }

        handleEvent(
                event,
                expectedPropertyId,
                propertySecretKey
        );
    }

    /**
     * Hàm điều phối chung cho cả SaaS Webhook và Property Webhook.
     *
     * expectedPropertyId == null:
     *     Luồng SaaS cũ.
     *
     * expectedPropertyId != null:
     *     Luồng Stripe riêng của Property.
     */
    private void handleEvent(
            Event event,
            Long expectedPropertyId,
            String propertySecretKey
    ) {

        if (event == null || !StringUtils.hasText(event.getType())) {
            throw new IllegalArgumentException(
                    "Stripe Event không hợp lệ"
            );
        }

        switch (event.getType()) {

            case "checkout.session.completed" -> {

                Session session = extractSession(event);

                if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {

                    handlePaymentSucceeded(
                            session,
                            expectedPropertyId,
                            propertySecretKey
                    );
                }

                // completed + unpaid có thể là phương thức thanh toán async.
            }

            case "checkout.session.async_payment_succeeded" -> {

                Session session = extractSession(event);

                handlePaymentSucceeded(
                        session,
                        expectedPropertyId,
                        propertySecretKey
                );
            }

            case "checkout.session.async_payment_failed" -> {

                Session session = extractSession(event);

                handlePaymentFailed(
                        session,
                        expectedPropertyId
                );
            }

            case "checkout.session.expired" -> {

                Session session = extractSession(event);

                handleCheckoutExpired(
                        session,
                        expectedPropertyId
                );
            }

            default -> {
                // Event không thuộc Reservation Checkout thì bỏ qua.
            }
        }
    }

    // =========================================================
    // 2. EXTRACT STRIPE SESSION
    // =========================================================

    private Session extractSession(Event event) {

        Optional<StripeObject> object =
                event.getDataObjectDeserializer().getObject();

        if (object.isEmpty()
                || !(object.get() instanceof Session session)) {

            throw new IllegalArgumentException(
                    "Không thể đọc Checkout Session từ Stripe Event: "
                            + event.getType()
            );
        }

        return session;
    }

    // =========================================================
    // 3. PAYMENT SUCCEEDED
    // =========================================================

    /**
     * Xử lý khi Stripe xác nhận thanh toán thành công.
     */
    private void handlePaymentSucceeded(
            Session session,
            Long expectedPropertyId,
            String propertySecretKey
    ) {

        // 1. Lock ReservationPayment.
        ReservationPayment payment = findAndLockPayment(session);

        // 2. Lock Reservation và xác minh Property nếu có.
        Reservation reservation =
                lockAndValidateReservation(
                        payment,
                        session,
                        expectedPropertyId
                );

        // 3. Xác minh Checkout Session.
        validateSession(
                session,
                payment,
                reservation
        );

        // 4. Lấy PaymentIntent ID.
        String paymentIntentId = session.getPaymentIntent();

        if (!StringUtils.hasText(paymentIntentId)) {
            throw new IllegalArgumentException(
                    "Checkout Session không có PaymentIntent ID"
            );
        }

        // 5. Retrieve PaymentIntent bằng đúng Stripe Secret Key.
        PaymentIntent intent = retrievePaymentIntent(
                paymentIntentId,
                propertySecretKey
        );

        // 6. Xác minh PaymentIntent.
        validatePaymentIntent(
                intent,
                payment,
                reservation,
                session
        );

        // 7. Không cho một PaymentIntent thuộc nhiều Payment.

        if (StringUtils.hasText(payment.getPaymentIntentId())
                && !paymentIntentId.equals(
                payment.getPaymentIntentId())) {

            throw new IllegalArgumentException(
                    "ReservationPayment đã lưu PaymentIntent ID khác"
            );
        }

        if (StringUtils.hasText(payment.getTransactionId())
                && !paymentIntentId.equals(
                payment.getTransactionId())) {

            throw new IllegalArgumentException(
                    "ReservationPayment đã lưu Transaction ID khác"
            );
        }

        paymentRepository.findByPaymentIntentId(paymentIntentId)
                .ifPresent(existing -> {

                    if (!existing.getReservationPaymentId()
                            .equals(payment.getReservationPaymentId())) {

                        throw new IllegalArgumentException(
                                "PaymentIntent đã liên kết với Reservation Payment khác"
                        );
                    }
                });

        // 8. Idempotency.
        // Webhook có thể được Stripe gửi lại nhiều lần.

        if (payment.getStatus() == ReservationPaymentStatus.SUCCEEDED) {

            if (!paymentIntentId.equals(payment.getPaymentIntentId())) {

                throw new IllegalArgumentException(
                        "Payment đã thành công với PaymentIntent khác"
                );
            }

            return;
        }

        // 9. Ghi nhận thanh toán.

        LocalDateTime paidAt = LocalDateTime.now();

        payment.setPaymentIntentId(paymentIntentId);
        payment.setTransactionId(paymentIntentId);
        payment.setPaidAt(paidAt);
        payment.setStatus(ReservationPaymentStatus.SUCCEEDED);

        if (StringUtils.hasText(intent.getPaymentMethod())) {
            payment.setPaymentMethod(intent.getPaymentMethod());
        }

        paymentRepository.save(payment);

        // 10. Chỉ confirm Reservation nếu vẫn còn hạn thanh toán.

        if (reservation.getStatus() == ReservationStatus.PENDING_PAYMENT) {

            if (reservation.getPaymentExpiresAt() != null
                    && reservation.getPaymentExpiresAt().isAfter(paidAt)) {

                reservation.setStatus(ReservationStatus.CONFIRMED);

                reservationRepository.save(reservation);

                // Listener chỉ gửi Email sau khi transaction commit.
                eventPublisher.publishEvent(
                        new ReservationConfirmedEvent(
                                reservation.getReservationId()
                        )
                );

            } else {

                reservation.setStatus(ReservationStatus.EXPIRED);

                reservationRepository.save(reservation);
            }
        }

        // Reservation đã EXPIRED/CANCELLED/... không tự động mở lại.
        // Không tự động refund khoản thanh toán đến muộn.
    }

    // =========================================================
    // 4. PAYMENT FAILED
    // =========================================================

    /**
     * Xử lý khi thanh toán thất bại.
     */
    private void handlePaymentFailed(
            Session session,
            Long expectedPropertyId
    ) {

        // 1. Lock Payment.
        ReservationPayment payment = findAndLockPayment(session);

        // 2. Lock Reservation và xác minh Property.
        Reservation reservation =
                lockAndValidateReservation(
                        payment,
                        session,
                        expectedPropertyId
                );

        // 3. Xác minh danh tính Checkout Session.
        validateSessionIdentity(
                session,
                payment,
                reservation
        );

        // 4. Không ghi đè trạng thái cuối cùng.

        if (payment.getStatus() == ReservationPaymentStatus.SUCCEEDED
                || payment.getStatus() == ReservationPaymentStatus.EXPIRED
                || payment.getStatus() == ReservationPaymentStatus.CANCELLED
                || payment.getStatus() == ReservationPaymentStatus.REFUNDED) {

            return;
        }

        // 5. Chỉ chuyển PENDING/PROCESSING thành FAILED.

        if (payment.getStatus() == ReservationPaymentStatus.PENDING
                || payment.getStatus() == ReservationPaymentStatus.PROCESSING) {

            payment.setStatus(ReservationPaymentStatus.FAILED);

            paymentRepository.save(payment);
        }
    }

    // =========================================================
    // 5. CHECKOUT SESSION EXPIRED
    // =========================================================

    /**
     * Xử lý khi Checkout Session hết hạn.
     */
    private void handleCheckoutExpired(
            Session session,
            Long expectedPropertyId
    ) {

        // 1. Lock Payment.
        ReservationPayment payment = findAndLockPayment(session);

        // 2. Lock Reservation và xác minh Property.
        Reservation reservation =
                lockAndValidateReservation(
                        payment,
                        session,
                        expectedPropertyId
                );

        // 3. Xác minh danh tính Checkout Session.
        validateSessionIdentity(
                session,
                payment,
                reservation
        );

        // 4. Không ghi đè trạng thái cuối cùng.

        if (payment.getStatus() == ReservationPaymentStatus.SUCCEEDED
                || payment.getStatus() == ReservationPaymentStatus.FAILED
                || payment.getStatus() == ReservationPaymentStatus.CANCELLED
                || payment.getStatus() == ReservationPaymentStatus.REFUNDED) {

            return;
        }

        // 5. Chuyển Payment thành EXPIRED.

        if (payment.getStatus() == ReservationPaymentStatus.PENDING
                || payment.getStatus() == ReservationPaymentStatus.PROCESSING) {

            payment.setStatus(ReservationPaymentStatus.EXPIRED);

            paymentRepository.save(payment);
        }

        // 6. Chỉ chuyển Reservation thành EXPIRED
        // nếu Reservation vẫn đang chờ thanh toán và đã hết hạn.

        if (reservation.getStatus() == ReservationStatus.PENDING_PAYMENT
                && (reservation.getPaymentExpiresAt() == null
                || !reservation.getPaymentExpiresAt().isAfter(
                LocalDateTime.now()))) {

            reservation.setStatus(ReservationStatus.EXPIRED);

            reservationRepository.save(reservation);
        }
    }

    // =========================================================
    // 6. FIND AND LOCK PAYMENT
    // =========================================================

    /**
     * Tìm và khóa ReservationPayment theo metadata của Stripe Session.
     */
    private ReservationPayment findAndLockPayment(Session session) {

        Map<String, String> metadata = session.getMetadata();

        if (metadata == null
                || !"RESERVATION".equals(
                metadata.get("payment_flow"))) {

            throw new IllegalArgumentException(
                    "Stripe Session không thuộc Reservation flow"
            );
        }

        Long paymentId = parseId(
                metadata.get("reservation_payment_id"),
                "reservation_payment_id"
        );

        ReservationPayment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy ReservationPayment: " + paymentId
                ));

        if (payment.getReservation() == null) {
            throw new IllegalArgumentException(
                    "ReservationPayment không liên kết Reservation"
            );
        }

        return payment;
    }

    // =========================================================
    // 7. LOCK AND VALIDATE RESERVATION
    // =========================================================

    /**
     * Lock Reservation và xác minh:
     * - ReservationPayment liên kết đúng Reservation.
     * - Metadata khớp Reservation.
     * - Reservation thuộc đúng Property đã xác minh Webhook.
     *
     * expectedPropertyId == null:
     *     Không kiểm tra Property ID bổ sung cho luồng SaaS cũ.
     */
    private Reservation lockAndValidateReservation(
            ReservationPayment payment,
            Session session,
            Long expectedPropertyId
    ) {

        Reservation paymentReservation = payment.getReservation();

        Reservation reservation = reservationRepository
                .findByReservationCodeForUpdate(
                        paymentReservation.getReservationCode()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy Reservation"
                ));

        Map<String, String> metadata = session.getMetadata();

        if (metadata == null) {
            throw new IllegalArgumentException(
                    "Stripe Session không có metadata"
            );
        }

        Long reservationId = parseId(
                metadata.get("reservation_id"),
                "reservation_id"
        );

        // 1. Đối chiếu Reservation ID và Reservation Code.

        if (!reservation.getReservationId().equals(reservationId)
                || !reservation.getReservationCode().equals(
                metadata.get("reservation_code"))
                || !paymentReservation.getReservationId().equals(
                reservation.getReservationId())) {

            throw new IllegalArgumentException(
                    "Reservation metadata không khớp Payment"
            );
        }

        // 2. Nếu đến từ Property Webhook, bắt buộc xác minh Property.

        if (expectedPropertyId != null) {

            if (reservation.getProperty() == null
                    || reservation.getProperty().getPropertyId() == null
                    || !expectedPropertyId.equals(
                    reservation.getProperty().getPropertyId())) {

                throw new IllegalArgumentException(
                        "Reservation không thuộc Property đã xác minh Webhook"
                );
            }
        }

        return reservation;
    }

    // =========================================================
    // 8. VALIDATE CHECKOUT SESSION
    // =========================================================

    /**
     * Xác minh Checkout Session và khoản Deposit.
     */
    private void validateSession(
            Session session,
            ReservationPayment payment,
            Reservation reservation
    ) {

        // Xác minh danh tính trước.
        validateSessionIdentity(
                session,
                payment,
                reservation
        );

        // Checkout Session phải hoàn tất và đã thanh toán.

        if (!"complete".equalsIgnoreCase(session.getStatus())
                || !"paid".equalsIgnoreCase(session.getPaymentStatus())) {

            throw new IllegalArgumentException(
                    "Checkout Session chưa được thanh toán hoàn tất"
            );
        }

        if (!"payment".equalsIgnoreCase(session.getMode())) {
            throw new IllegalArgumentException(
                    "Checkout Session không ở payment mode"
            );
        }

        // Xác minh Amount.

        long expected = toStripeAmount(
                payment.getAmount(),
                payment.getCurrency()
        );

        if (session.getAmountTotal() == null
                || session.getAmountTotal() != expected) {

            throw new IllegalArgumentException(
                    "Amount của Checkout Session không khớp Payment"
            );
        }

        // Xác minh Currency.

        if (!StringUtils.hasText(session.getCurrency())
                || !payment.getCurrency().equalsIgnoreCase(
                session.getCurrency())) {

            throw new IllegalArgumentException(
                    "Currency của Checkout Session không khớp Payment"
            );
        }

        if (payment.getPaymentType() == ReservationPaymentType.DEPOSIT) {

            if (payment.getAmount() == null
                    || reservation.getDepositAmount() == null
                    || payment.getAmount().compareTo(
                    reservation.getDepositAmount()) != 0
                    || !payment.getCurrency().equalsIgnoreCase(
                    reservation.getCurrency())) {

                throw new IllegalArgumentException(
                        "Reservation Deposit không khớp ReservationPayment"
                );
            }

        } else if (payment.getPaymentType()
                == ReservationPaymentType.BALANCE) {

            if (payment.getAmount() == null
                    || payment.getAmount().signum() <= 0
                    || !payment.getCurrency().equalsIgnoreCase(
                    reservation.getCurrency())) {

                throw new IllegalArgumentException(
                        "Reservation Balance không khớp ReservationPayment"
                );
            }

        } else {

            throw new IllegalArgumentException(
                    "Payment Type không được hỗ trợ cho Reservation Checkout"
            );
        }
    }

    // =========================================================
    // 9. VALIDATE CHECKOUT SESSION IDENTITY
    // =========================================================

    /**
     * Đối chiếu Checkout Session với dữ liệu đã lưu trong Database.
     */
    private void validateSessionIdentity(
            Session session,
            ReservationPayment payment,
            Reservation reservation
    ) {

        Map<String, String> metadata = session.getMetadata();

        String expectedReference =
                "RESERVATION_PAYMENT_"
                        + payment.getReservationPaymentId();

        // 1. Checkout Session ID.

        if (!StringUtils.hasText(session.getId())
                || !session.getId().equals(
                payment.getCheckoutSessionId())) {

            throw new IllegalArgumentException(
                    "Checkout Session ID không khớp Payment"
            );
        }

        // 2. Client Reference ID.

        if (!expectedReference.equals(
                session.getClientReferenceId())) {

            throw new IllegalArgumentException(
                    "Checkout Session client_reference_id không khớp"
            );
        }

        // 3. Metadata.

        if (metadata == null
                || !"RESERVATION".equals(
                metadata.get("payment_flow"))
                || !payment.getReservationPaymentId().toString().equals(
                metadata.get("reservation_payment_id"))
                || !reservation.getReservationId().toString().equals(
                metadata.get("reservation_id"))
                || !reservation.getReservationCode().equals(
                metadata.get("reservation_code"))) {

            throw new IllegalArgumentException(
                    "Checkout Session metadata không khớp Payment"
            );
        }
    }

    // =========================================================
    // 10. RETRIEVE PAYMENT INTENT
    // =========================================================

    /**
     * Retrieve PaymentIntent.
     *
     * propertySecretKey == null:
     *     Sử dụng SaaS Stripe Secret Key thông qua Gateway cũ.
     *
     * propertySecretKey != null:
     *     Sử dụng Stripe Secret Key riêng của Property.
     */
    private PaymentIntent retrievePaymentIntent(
            String paymentIntentId,
            String propertySecretKey
    ) {

        try {

            if (StringUtils.hasText(propertySecretKey)) {

                return paymentIntentGateway.retrievePaymentIntent(
                        paymentIntentId,
                        propertySecretKey
                );
            }

            return paymentIntentGateway.retrievePaymentIntent(
                    paymentIntentId
            );

        } catch (StripeException exception) {

            throw new IllegalStateException(
                    "Không thể retrieve Stripe PaymentIntent",
                    exception
            );
        }
    }

    // =========================================================
    // 11. VALIDATE PAYMENT INTENT
    // =========================================================

    /**
     * Xác minh PaymentIntent với ReservationPayment.
     */
    private void validatePaymentIntent(
            PaymentIntent intent,
            ReservationPayment payment,
            Reservation reservation,
            Session session
    ) {

        // 1. PaymentIntent phải thành công.

        if (intent == null
                || !"succeeded".equalsIgnoreCase(intent.getStatus())) {

            throw new IllegalArgumentException(
                    "Stripe PaymentIntent chưa ở trạng thái succeeded"
            );
        }

        // 2. PaymentIntent ID phải khớp Checkout Session.

        if (!StringUtils.hasText(session.getPaymentIntent())
                || !session.getPaymentIntent().equals(intent.getId())) {

            throw new IllegalArgumentException(
                    "PaymentIntent ID không khớp Checkout Session"
            );
        }

        // 3. Xác minh Amount và Amount Received.

        long expected = toStripeAmount(
                payment.getAmount(),
                payment.getCurrency()
        );

        if (intent.getAmount() == null
                || intent.getAmount() != expected
                || intent.getAmountReceived() == null
                || intent.getAmountReceived() != expected) {

            throw new IllegalArgumentException(
                    "Amount của PaymentIntent không khớp Payment"
            );
        }

        // 4. Xác minh Currency.

        if (!StringUtils.hasText(intent.getCurrency())
                || !payment.getCurrency().equalsIgnoreCase(
                intent.getCurrency())) {

            throw new IllegalArgumentException(
                    "Currency của PaymentIntent không khớp Payment"
            );
        }

        // 5. Nếu PaymentIntent có metadata thì đối chiếu.
        // Session metadata vẫn là liên kết bắt buộc
        // để tương thích với các Session cũ.

        Map<String, String> intentMetadata = intent.getMetadata();

        if (intentMetadata != null && !intentMetadata.isEmpty()) {

            if (!"RESERVATION".equals(
                    intentMetadata.get("payment_flow"))
                    || !payment.getReservationPaymentId().toString().equals(
                    intentMetadata.get("reservation_payment_id"))
                    || !reservation.getReservationId().toString().equals(
                    intentMetadata.get("reservation_id"))
                    || !reservation.getReservationCode().equals(
                    intentMetadata.get("reservation_code"))) {

                throw new IllegalArgumentException(
                        "PaymentIntent metadata không khớp Reservation"
                );
            }
        }
    }

    // =========================================================
    // 12. PARSE STRIPE METADATA ID
    // =========================================================

    private Long parseId(
            String value,
            String field
    ) {

        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(
                    "Thiếu Stripe metadata: " + field
            );
        }

        try {

            return Long.valueOf(value);

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "Stripe metadata không hợp lệ: " + field,
                    exception
            );
        }
    }

    // =========================================================
    // 13. CONVERT AMOUNT TO STRIPE MINOR UNIT
    // =========================================================

    /**
     * Chuyển amount trong Database sang đơn vị nhỏ nhất Stripe yêu cầu.
     *
     * VND, JPY, KRW: không nhân 100.
     * Các currency còn lại: nhân 100.
     */
    private long toStripeAmount(
            BigDecimal amount,
            String currency
    ) {

        if (amount == null
                || amount.signum() <= 0
                || !StringUtils.hasText(currency)) {

            throw new IllegalArgumentException(
                    "Reservation Payment amount/currency không hợp lệ"
            );
        }

        String normalized = currency.toUpperCase(Locale.ROOT);

        if ("VND".equals(normalized)
                || "JPY".equals(normalized)
                || "KRW".equals(normalized)) {

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
