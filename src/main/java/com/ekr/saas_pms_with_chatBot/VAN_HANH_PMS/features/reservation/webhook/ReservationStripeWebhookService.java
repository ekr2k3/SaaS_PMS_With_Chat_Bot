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

    /**
     * Event đã được xác minh chữ ký bởi StripeWebhookRouterService.
     */
    @Transactional
    public void handleVerifiedEvent(Event event) {

        Session session = extractSession(event);

        switch (event.getType()) {

            case "checkout.session.completed" -> {
                if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
                    handlePaymentSucceeded(session);
                }
                // completed + unpaid có thể là phương thức thanh toán async.
            }

            case "checkout.session.async_payment_succeeded" ->
                    handlePaymentSucceeded(session);

            case "checkout.session.async_payment_failed" ->
                    handlePaymentFailed(session);

            case "checkout.session.expired" ->
                    handleCheckoutExpired(session);

            default -> {
                // Không thuộc phạm vi Reservation Checkout.
            }
        }
    }

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

    /**
     * Thanh toán thành công.
     */
    private void handlePaymentSucceeded(Session session) {

        // 1. Lock Payment.
        ReservationPayment payment = findAndLockPayment(session);

        // 2. Lock Reservation.
        Reservation reservation =
                lockAndValidateReservation(payment, session);

        // 3. Kiểm tra Checkout Session.
        validateSession(session, payment, reservation);

        // 4. Lấy PaymentIntent ID.
        String paymentIntentId = session.getPaymentIntent();

        if (!StringUtils.hasText(paymentIntentId)) {
            throw new IllegalArgumentException(
                    "Checkout Session không có PaymentIntent ID"
            );
        }

        // 5. Retrieve PaymentIntent từ Stripe.
        PaymentIntent intent = retrievePaymentIntent(paymentIntentId);

        // 6. Xác minh PaymentIntent.
        validatePaymentIntent(
                intent,
                payment,
                reservation,
                session
        );

        // 7. Không cho một PaymentIntent thuộc nhiều Payment.
        if (StringUtils.hasText(payment.getPaymentIntentId())
                && !paymentIntentId.equals(payment.getPaymentIntentId())) {

            throw new IllegalArgumentException(
                    "ReservationPayment đã lưu PaymentIntent ID khác"
            );
        }

        if (StringUtils.hasText(payment.getTransactionId())
                && !paymentIntentId.equals(payment.getTransactionId())) {

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

        // 10. Chỉ confirm nếu Reservation vẫn còn hạn.

        if (reservation.getStatus() == ReservationStatus.PENDING_PAYMENT) {

            if (reservation.getPaymentExpiresAt() != null
                    && reservation.getPaymentExpiresAt().isAfter(paidAt)) {

                reservation.setStatus(ReservationStatus.CONFIRMED);

                reservationRepository.save(reservation);

                // Phát Event sau khi Reservation được xác nhận.
                // Email Listener chỉ thực thi sau khi transaction commit.
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

        // Reservation đã EXPIRED/CANCELLED/... không được tự động mở lại.
        // Không tự động refund khoản thanh toán đến muộn.
    }

    /**
     * Thanh toán thất bại.
     */
    private void handlePaymentFailed(Session session) {

        ReservationPayment payment = findAndLockPayment(session);

        Reservation reservation =
                lockAndValidateReservation(payment, session);

        validateSessionIdentity(session, payment, reservation);

        if (payment.getStatus() == ReservationPaymentStatus.SUCCEEDED
                || payment.getStatus() == ReservationPaymentStatus.EXPIRED
                || payment.getStatus() == ReservationPaymentStatus.CANCELLED) {

            return;
        }

        if (payment.getStatus() == ReservationPaymentStatus.PENDING
                || payment.getStatus() == ReservationPaymentStatus.PROCESSING) {

            payment.setStatus(ReservationPaymentStatus.FAILED);

            paymentRepository.save(payment);
        }
    }

    /**
     * Checkout Session hết hạn.
     */
    private void handleCheckoutExpired(Session session) {

        ReservationPayment payment = findAndLockPayment(session);

        Reservation reservation =
                lockAndValidateReservation(payment, session);

        validateSessionIdentity(session, payment, reservation);

        if (payment.getStatus() == ReservationPaymentStatus.SUCCEEDED
                || payment.getStatus() == ReservationPaymentStatus.FAILED
                || payment.getStatus() == ReservationPaymentStatus.CANCELLED) {

            return;
        }

        if (payment.getStatus() == ReservationPaymentStatus.PENDING
                || payment.getStatus() == ReservationPaymentStatus.PROCESSING) {

            payment.setStatus(ReservationPaymentStatus.EXPIRED);

            paymentRepository.save(payment);
        }

        if (reservation.getStatus() == ReservationStatus.PENDING_PAYMENT
                && (reservation.getPaymentExpiresAt() == null
                || !reservation.getPaymentExpiresAt().isAfter(LocalDateTime.now()))) {

            reservation.setStatus(ReservationStatus.EXPIRED);

            reservationRepository.save(reservation);
        }
    }

    /**
     * Tìm và khóa ReservationPayment theo metadata.
     */
    private ReservationPayment findAndLockPayment(Session session) {

        Map<String, String> metadata = session.getMetadata();

        if (metadata == null
                || !"RESERVATION".equals(metadata.get("payment_flow"))) {

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

    /**
     * Lock và xác minh Reservation.
     */
    private Reservation lockAndValidateReservation(
            ReservationPayment payment,
            Session session
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

        Long reservationId = parseId(
                metadata.get("reservation_id"),
                "reservation_id"
        );

        if (!reservation.getReservationId().equals(reservationId)
                || !reservation.getReservationCode().equals(
                metadata.get("reservation_code"))
                || !paymentReservation.getReservationId().equals(
                reservation.getReservationId())) {

            throw new IllegalArgumentException(
                    "Reservation metadata không khớp Payment"
            );
        }

        return reservation;
    }

    /**
     * Xác minh Checkout Session và khoản Deposit.
     */
    private void validateSession(
            Session session,
            ReservationPayment payment,
            Reservation reservation
    ) {

        validateSessionIdentity(session, payment, reservation);

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

        if (!StringUtils.hasText(session.getCurrency())
                || !payment.getCurrency().equalsIgnoreCase(
                session.getCurrency())) {

            throw new IllegalArgumentException(
                    "Currency của Checkout Session không khớp Payment"
            );
        }

        if (payment.getPaymentType() != ReservationPaymentType.DEPOSIT
                || payment.getAmount() == null
                || reservation.getDepositAmount() == null
                || payment.getAmount().compareTo(
                reservation.getDepositAmount()) != 0
                || !payment.getCurrency().equalsIgnoreCase(
                reservation.getCurrency())) {

            throw new IllegalArgumentException(
                    "Reservation Deposit không khớp ReservationPayment"
            );
        }
    }

    /**
     * Đối chiếu Checkout Session với dữ liệu đã lưu.
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

        if (!StringUtils.hasText(session.getId())
                || !session.getId().equals(
                payment.getCheckoutSessionId())) {

            throw new IllegalArgumentException(
                    "Checkout Session ID không khớp Payment"
            );
        }

        if (!expectedReference.equals(
                session.getClientReferenceId())) {

            throw new IllegalArgumentException(
                    "Checkout Session client_reference_id không khớp"
            );
        }

        if (metadata == null
                || !"RESERVATION".equals(metadata.get("payment_flow"))
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

    /**
     * Retrieve PaymentIntent.
     */
    private PaymentIntent retrievePaymentIntent(String paymentIntentId) {

        try {
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

    /**
     * Xác minh PaymentIntent.
     */
    private void validatePaymentIntent(
            PaymentIntent intent,
            ReservationPayment payment,
            Reservation reservation,
            Session session
    ) {

        if (intent == null
                || !"succeeded".equalsIgnoreCase(intent.getStatus())) {

            throw new IllegalArgumentException(
                    "Stripe PaymentIntent chưa ở trạng thái succeeded"
            );
        }

        if (!StringUtils.hasText(session.getPaymentIntent())
                || !session.getPaymentIntent().equals(intent.getId())) {

            throw new IllegalArgumentException(
                    "PaymentIntent ID không khớp Checkout Session"
            );
        }

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

        if (!StringUtils.hasText(intent.getCurrency())
                || !payment.getCurrency().equalsIgnoreCase(
                intent.getCurrency())) {

            throw new IllegalArgumentException(
                    "Currency của PaymentIntent không khớp Payment"
            );
        }

        // Nếu PaymentIntent có metadata thì đối chiếu.
        // Session metadata vẫn là liên kết bắt buộc để tương thích Session cũ.
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

    private Long parseId(String value, String field) {

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

    /**
     * Chuyển amount DB sang đơn vị nhỏ nhất Stripe yêu cầu.
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