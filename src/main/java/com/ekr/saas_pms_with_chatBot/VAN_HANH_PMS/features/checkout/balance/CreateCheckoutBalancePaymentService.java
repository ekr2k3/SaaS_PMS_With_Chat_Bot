package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.balance;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationPaymentRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.CheckoutAccessService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CreateCheckoutBalancePaymentService {

    private static final String REQUIRED_PERMISSION = "PAYMENT_CREATE";

    private final CheckoutAccessService accessService;

    private final ReservationRepository reservationRepository;

    private final ReservationPaymentRepository paymentRepository;

    private final GetCheckoutBalanceService balanceService;

    public CreateCheckoutBalancePaymentService(
            CheckoutAccessService accessService,
            ReservationRepository reservationRepository,
            ReservationPaymentRepository paymentRepository,
            GetCheckoutBalanceService balanceService
    ) {
        this.accessService = accessService;
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
        this.balanceService = balanceService;
    }

    @Transactional
    public CreateCheckoutBalancePaymentResponse createPayment(
            Long propertyId,
            String reservationCode
    ) {

        accessService.requirePermission(
                propertyId,
                REQUIRED_PERMISSION
        );

        if (!StringUtils.hasText(reservationCode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation Code không hợp lệ"
            );
        }

        Reservation reservation = reservationRepository
                .findByReservationCodeForUpdate(reservationCode.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reservation not found"
                ));

        if (reservation.getProperty() == null
                || !propertyId.equals(
                        reservation.getProperty().getPropertyId()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reservation not found"
            );
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation phải ở trạng thái CHECKED_IN"
            );
        }

        String currency = reservation.getCurrency();

        if (!StringUtils.hasText(currency)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation chưa xác định Currency"
            );
        }

        GetCheckoutBalanceResponse balance =
                balanceService.calculateBalance(reservation);

        BigDecimal balanceDue = balance.balanceDue();

        if (balanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation không còn số tiền cần thanh toán"
            );
        }

        List<ReservationPayment> payments =
                paymentRepository
                        .findAllByReservation_ReservationIdOrderByReservationPaymentIdDesc(
                                reservation.getReservationId()
                        );

        ReservationPayment existingAttempt = payments.stream()
                .filter(payment ->
                        payment.getPaymentType()
                                == ReservationPaymentType.BALANCE
                )
                .filter(payment ->
                        payment.getStatus()
                                == ReservationPaymentStatus.PENDING
                                || payment.getStatus()
                                == ReservationPaymentStatus.PROCESSING
                )
                .findFirst()
                .orElse(null);

        if (existingAttempt != null) {

            if (existingAttempt.getAmount() == null
                    || existingAttempt.getAmount()
                    .compareTo(balanceDue) != 0) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Đã tồn tại BALANCE Payment với số tiền khác. "
                                + "Cần xử lý Payment Attempt hiện tại trước."
                );
            }

            return toResponse(
                    existingAttempt,
                    true
            );
        }

        ReservationPayment payment = new ReservationPayment();

        payment.setReservation(reservation);
        payment.setPaymentType(
                ReservationPaymentType.BALANCE
        );
        payment.setAmount(balanceDue);
        payment.setCurrency(currency);
        payment.setStatus(
                ReservationPaymentStatus.PENDING
        );

        ReservationPayment savedPayment =
                paymentRepository.save(payment);

        return toResponse(
                savedPayment,
                false
        );
    }

    private CreateCheckoutBalancePaymentResponse toResponse(
            ReservationPayment payment,
            boolean reusedExistingAttempt
    ) {

        return new CreateCheckoutBalancePaymentResponse(
                payment.getReservationPaymentId(),
                payment.getReservation().getReservationCode(),
                payment.getPaymentType(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                reusedExistingAttempt
        );
    }
}
