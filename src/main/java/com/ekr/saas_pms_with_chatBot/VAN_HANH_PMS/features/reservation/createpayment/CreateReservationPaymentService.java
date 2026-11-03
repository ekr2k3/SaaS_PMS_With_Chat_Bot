package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.createpayment;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Guest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationGuestRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationPaymentRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateReservationPaymentService {

    private final ReservationRepository reservationRepository;

    private final ReservationGuestRepository reservationGuestRepository;

    private final ReservationPaymentRepository reservationPaymentRepository;

    private final CreateReservationPaymentValidator validator;


    @Transactional
    public CreateReservationPaymentResponse createPayment(
            String reservationCode,
            String email,
            String phone
    ) {

        validator.validate(reservationCode, email, phone);

        // 1. Lock Reservation để tránh tạo nhiều Payment đồng thời.
        Reservation reservation = reservationRepository
                .findByReservationCodeForUpdate(reservationCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Reservation"
                ));

        // 2. Xác thực Guest.
        validateGuestIdentity(
                reservation.getReservationId(),
                email,
                phone
        );

        // 3. Kiểm tra trạng thái Reservation.
        if (reservation.getStatus() != ReservationStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation không ở trạng thái chờ thanh toán"
            );
        }

        // 4. Kiểm tra thời hạn giữ phòng.
        LocalDateTime now = LocalDateTime.now();

        if (reservation.getPaymentExpiresAt() == null ||
                !reservation.getPaymentExpiresAt().isAfter(now)) {

            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Thời gian giữ phòng đã hết hạn"
            );
        }

        // 5. Lấy lịch sử Payment.
        List<ReservationPayment> payments =
                reservationPaymentRepository
                        .findAllByReservation_ReservationIdOrderByReservationPaymentIdDesc(
                                reservation.getReservationId()
                        );

        // 6. Không cho thanh toán lần nữa nếu đã thành công.
        boolean hasSuccessfulPayment = payments.stream()
                .anyMatch(payment ->
                        payment.getStatus() == ReservationPaymentStatus.SUCCEEDED
                );

        if (hasSuccessfulPayment) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation đã có Payment thành công"
            );
        }

        // 7. Nếu đã có Payment đang xử lý, tái sử dụng.
        ReservationPayment existingAttempt = payments.stream()
                .filter(payment ->
                        payment.getStatus() == ReservationPaymentStatus.PENDING ||
                        payment.getStatus() == ReservationPaymentStatus.PROCESSING
                )
                .findFirst()
                .orElse(null);

        if (existingAttempt != null) {
            return toResponse(existingAttempt, true);
        }

        // 8. Lấy số tiền Deposit từ Reservation.
        BigDecimal depositAmount = reservation.getDepositAmount();

        if (depositAmount == null ||
                depositAmount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation không có khoản Deposit cần thanh toán"
            );
        }

        String currency = reservation.getCurrency();

        if (currency == null || currency.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation chưa xác định Currency"
            );
        }

        // 9. Tạo Payment Attempt mới.
        ReservationPayment payment = new ReservationPayment();

        payment.setReservation(reservation);
        payment.setPaymentType(ReservationPaymentType.DEPOSIT);
        payment.setAmount(depositAmount);
        payment.setCurrency(currency);
        payment.setStatus(ReservationPaymentStatus.PENDING);

        ReservationPayment savedPayment =
                reservationPaymentRepository.save(payment);

        return toResponse(savedPayment, false);
    }


    private void validateGuestIdentity(
            Long reservationId,
            String email,
            String phone
    ) {

        List<ReservationGuest> reservationGuests =
                reservationGuestRepository.findAllByReservationId(reservationId);

        String normalizedEmail =
                email == null || email.isBlank()
                        ? null
                        : email.trim();

        String normalizedPhone =
                phone == null || phone.isBlank()
                        ? null
                        : phone.trim();

        boolean matched = reservationGuests.stream()
                .map(ReservationGuest::getGuest)
                .anyMatch(guest ->
                        matchesIdentity(
                                guest,
                                normalizedEmail,
                                normalizedPhone
                        )
                );

        if (!matched) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy Reservation phù hợp với thông tin xác thực"
            );
        }
    }


    private boolean matchesIdentity(
            Guest guest,
            String email,
            String phone
    ) {

        boolean emailMatches = email == null ||
                (guest.getEmail() != null &&
                        guest.getEmail().equalsIgnoreCase(email));

        boolean phoneMatches = phone == null ||
                (guest.getPhone() != null &&
                        guest.getPhone().equals(phone));

        return emailMatches && phoneMatches;
    }


    private CreateReservationPaymentResponse toResponse(
            ReservationPayment payment,
            boolean reusedExistingAttempt
    ) {

        return new CreateReservationPaymentResponse(
                payment.getReservationPaymentId(),
                payment.getPaymentType(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                reusedExistingAttempt
        );
    }
}
