package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.checkout;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Guest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationGuestRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationPaymentRepository;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CreateReservationCheckoutSessionService {

    private final ReservationPaymentRepository paymentRepository;

    private final ReservationGuestRepository reservationGuestRepository;

    private final ReservationStripeCheckoutGateway stripeGateway;


    public CreateReservationCheckoutSessionService(
            ReservationPaymentRepository paymentRepository,
            ReservationGuestRepository reservationGuestRepository,
            ReservationStripeCheckoutGateway stripeGateway
    ) {
        this.paymentRepository = paymentRepository;
        this.reservationGuestRepository = reservationGuestRepository;
        this.stripeGateway = stripeGateway;
    }


    /**
     * noRollbackFor được dùng trong trường hợp Session trên Stripe
     * đã expired: cần commit trạng thái Payment = EXPIRED trước khi
     * trả lỗi 409 cho Frontend.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public CreateReservationCheckoutSessionResponse createCheckoutSession(
            String reservationCode,
            Long paymentId,
            String email,
            String phone
    ) {

        validateRequest(
                reservationCode,
                paymentId,
                email,
                phone
        );

        // 1. Lock Payment.
        ReservationPayment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Reservation Payment"
                ));

        Reservation reservation = payment.getReservation();

        // 2. Payment phải thuộc Reservation trên URL.
        if (!reservation.getReservationCode().equals(reservationCode)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy Reservation Payment"
            );
        }

        // 3. Xác thực Guest.
        validateGuest(
                reservation.getReservationId(),
                email,
                phone
        );

        // 4. Reservation phải đang chờ thanh toán.
        if (reservation.getStatus()
                != ReservationStatus.PENDING_PAYMENT) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation không ở trạng thái chờ thanh toán"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (reservation.getPaymentExpiresAt() == null
                || !reservation.getPaymentExpiresAt().isAfter(now)) {

            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Thời gian giữ phòng đã hết hạn"
            );
        }

        // 5. Chỉ xử lý Deposit trong Slice này.
        if (payment.getPaymentType()
                != ReservationPaymentType.DEPOSIT) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment không phải Deposit Payment"
            );
        }

        validatePaymentAmount(payment, reservation);

        // 6. Payment đã thành công thì không tạo Checkout lần nữa.
        if (payment.getStatus() == ReservationPaymentStatus.SUCCEEDED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment đã được thanh toán thành công"
            );
        }

        if (payment.getStatus() == ReservationPaymentStatus.FAILED
                || payment.getStatus() == ReservationPaymentStatus.CANCELLED
                || payment.getStatus() == ReservationPaymentStatus.EXPIRED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment Attempt không còn hiệu lực. Vui lòng tạo Payment Attempt mới"
            );
        }

        if (payment.getStatus() != ReservationPaymentStatus.PENDING
                && payment.getStatus() != ReservationPaymentStatus.PROCESSING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Trạng thái Payment không hợp lệ"
            );
        }

        // 7. Đã có Session ID thì không tạo Session khác.
        if (StringUtils.hasText(payment.getCheckoutSessionId())) {

            return retrieveExistingSession(payment);
        }

        // 8. Chưa có Session ID.
        // Chỉ Payment PENDING mới được phép tạo Session.
        if (payment.getStatus() != ReservationPaymentStatus.PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment đang xử lý nhưng chưa có Checkout Session"
            );
        }

        return createNewSession(payment, reservation);
    }


    private CreateReservationCheckoutSessionResponse createNewSession(
            ReservationPayment payment,
            Reservation reservation
    ) {

        String currency = payment.getCurrency();

        long stripeAmount = toStripeAmount(
                payment.getAmount(),
                currency
        );

        String idempotencyKey =
                "reservation-payment-" + payment.getReservationPaymentId();

        String productName =
                "Reservation Deposit - " + reservation.getReservationCode();

        try {

            Session session = stripeGateway.createCheckoutSession(
                    payment.getReservationPaymentId(),
                    reservation.getReservationId(),
                    reservation.getReservationCode(),
                    productName,
                    stripeAmount,
                    currency,
                    idempotencyKey
            );

            if (session == null
                    || !StringUtils.hasText(session.getId())
                    || !StringUtils.hasText(session.getUrl())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Stripe không trả về Checkout Session hợp lệ"
                );
            }

            // Lưu Session ID để các request sau dùng lại Session.
            payment.setCheckoutSessionId(session.getId());

            // Stripe có thể chưa tạo PaymentIntent tại thời điểm này.
            // Vì vậy chỉ lưu nếu Stripe thực sự trả về ID.
            if (StringUtils.hasText(session.getPaymentIntent())) {
                payment.setPaymentIntentId(
                        session.getPaymentIntent()
                );
            }

            paymentRepository.saveAndFlush(payment);

            return toResponse(
                    payment,
                    session.getUrl(),
                    false
            );

        } catch (StripeException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể tạo Stripe Checkout Session",
                    exception
            );
        }
    }


    private CreateReservationCheckoutSessionResponse retrieveExistingSession(
            ReservationPayment payment
    ) {

        try {

            Session session = stripeGateway.retrieveCheckoutSession(
                    payment.getCheckoutSessionId()
            );

            if (session == null || !StringUtils.hasText(session.getStatus())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Không thể xác định trạng thái Stripe Checkout Session"
                );
            }

            String sessionStatus = session.getStatus();

            // Session vẫn mở: trả URL cũ.
            if ("open".equalsIgnoreCase(sessionStatus)) {

                if (!StringUtils.hasText(session.getUrl())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Stripe Checkout Session không có URL"
                    );
                }

                return toResponse(
                        payment,
                        session.getUrl(),
                        true
                );
            }

            // Session đã hoàn tất: chờ Webhook cập nhật Payment.
            if ("complete".equalsIgnoreCase(sessionStatus)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Checkout Session đã hoàn tất. Vui lòng chờ hệ thống xác nhận thanh toán"
                );
            }

            // Session đã hết hạn: đóng Payment Attempt này.
            if ("expired".equalsIgnoreCase(sessionStatus)) {

                payment.setStatus(
                        ReservationPaymentStatus.EXPIRED
                );

                paymentRepository.saveAndFlush(payment);

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Checkout Session đã hết hạn. Vui lòng tạo Payment Attempt mới"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Stripe trả về trạng thái Checkout Session không xác định"
            );

        } catch (StripeException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể truy vấn Checkout Session từ Stripe",
                    exception
            );
        }
    }


    private void validatePaymentAmount(
            ReservationPayment payment,
            Reservation reservation
    ) {

        BigDecimal paymentAmount = payment.getAmount();
        BigDecimal depositAmount = reservation.getDepositAmount();

        if (paymentAmount == null
                || depositAmount == null
                || paymentAmount.signum() <= 0
                || paymentAmount.compareTo(depositAmount) != 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Số tiền Payment không khớp với Deposit của Reservation"
            );
        }

        String paymentCurrency = payment.getCurrency();
        String reservationCurrency = reservation.getCurrency();

        if (!StringUtils.hasText(paymentCurrency)
                || !StringUtils.hasText(reservationCurrency)
                || !paymentCurrency.equalsIgnoreCase(reservationCurrency)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Currency của Payment không khớp Reservation"
            );
        }
    }


    private void validateGuest(
            Long reservationId,
            String email,
            String phone
    ) {

        List<ReservationGuest> reservationGuests =
                reservationGuestRepository.findAllByReservationId(
                        reservationId
                );

        String normalizedEmail =
                StringUtils.hasText(email)
                        ? email.trim()
                        : null;

        String normalizedPhone =
                StringUtils.hasText(phone)
                        ? phone.trim()
                        : null;

        boolean matched = reservationGuests.stream()
                .map(ReservationGuest::getGuest)
                .anyMatch(guest -> matchesGuest(
                        guest,
                        normalizedEmail,
                        normalizedPhone
                ));

        if (!matched) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy Reservation phù hợp với thông tin Guest"
            );
        }
    }


    private boolean matchesGuest(
            Guest guest,
            String email,
            String phone
    ) {

        boolean emailMatches =
                email == null
                        || (guest.getEmail() != null
                        && guest.getEmail().equalsIgnoreCase(email));

        boolean phoneMatches =
                phone == null
                        || (guest.getPhone() != null
                        && guest.getPhone().equals(phone));

        return emailMatches && phoneMatches;
    }


    private void validateRequest(
            String reservationCode,
            Long paymentId,
            String email,
            String phone
    ) {

        if (!StringUtils.hasText(reservationCode)
                || paymentId == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation Code hoặc Payment ID không hợp lệ"
            );
        }

        if (!StringUtils.hasText(email)
                && !StringUtils.hasText(phone)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phải cung cấp Email hoặc Phone"
            );
        }

        if (StringUtils.hasText(email)) {

            String normalizedEmail = email.trim();

            if (normalizedEmail.length() > 255
                    || !normalizedEmail.matches(
                            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
                    )) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Email không hợp lệ"
                );
            }
        }

        if (StringUtils.hasText(phone)
                && phone.trim().length() > 20) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone không hợp lệ"
            );
        }
    }


    /**
     * Stripe yêu cầu số tiền theo đơn vị nhỏ nhất của currency.
     *
     * VND, JPY, KRW: không nhân 100.
     * Các currency còn lại: nhân 100.
     *
     * Không làm tròn âm thầm; số tiền có phần lẻ không hợp lệ
     * sẽ phát sinh ArithmeticException.
     */
    private long toStripeAmount(
            BigDecimal amount,
            String currency
    ) {

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


    private CreateReservationCheckoutSessionResponse toResponse(
            ReservationPayment payment,
            String checkoutUrl,
            boolean reusedExistingSession
    ) {

        return new CreateReservationCheckoutSessionResponse(
                payment.getReservationPaymentId(),
                payment.getReservation().getReservationCode(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCheckoutSessionId(),
                checkoutUrl,
                reusedExistingSession
        );
    }
}
