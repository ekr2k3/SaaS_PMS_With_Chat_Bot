package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.balance;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Folio;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.FolioRepository;
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
public class GetCheckoutBalanceService {

    private static final String REQUIRED_PERMISSION = "CHECKOUT_VIEW";

    private final CheckoutAccessService accessService;

    private final ReservationRepository reservationRepository;

    private final FolioRepository folioRepository;

    private final ReservationPaymentRepository paymentRepository;

    public GetCheckoutBalanceService(
            CheckoutAccessService accessService,
            ReservationRepository reservationRepository,
            FolioRepository folioRepository,
            ReservationPaymentRepository paymentRepository
    ) {
        this.accessService = accessService;
        this.reservationRepository = reservationRepository;
        this.folioRepository = folioRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * API dành cho PMS Employee.
     */
    @Transactional(readOnly = true)
    public GetCheckoutBalanceResponse getBalance(
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
                .findByReservationCode(reservationCode.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reservation not found"
                ));

        validateReservationProperty(
                reservation,
                propertyId
        );

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation phải ở trạng thái CHECKED_IN"
            );
        }

        return calculateBalance(reservation);
    }

    /**
     * Hàm tính toán nội bộ được tái sử dụng bởi các luồng Checkout.
     */
    @Transactional(readOnly = true)
    public GetCheckoutBalanceResponse calculateBalance(
            Reservation reservation
    ) {

        if (reservation == null) {
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

        Long reservationId = reservation.getReservationId();

        BigDecimal roomAmount = zeroIfNull(
                reservation.getTotalAmount()
        );

        List<Folio> folios =
                folioRepository
                        .findAllByReservation_ReservationIdOrderByFolioIdAsc(
                                reservationId
                        );

        BigDecimal folioTotal = folios.stream()
                .map(Folio::getAmount)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCharge =
                roomAmount.add(folioTotal);

        List<ReservationPayment> payments =
                paymentRepository
                        .findAllByReservation_ReservationIdOrderByReservationPaymentIdDesc(
                                reservationId
                        );

        BigDecimal totalCapturedAmount = BigDecimal.ZERO;
        BigDecimal totalRefundedAmount = BigDecimal.ZERO;
        BigDecimal pendingRefundAmount = BigDecimal.ZERO;

        for (ReservationPayment payment : payments) {

            ReservationPaymentType type = payment.getPaymentType();
            ReservationPaymentStatus status = payment.getStatus();
            BigDecimal amount = zeroIfNull(payment.getAmount());

            if ((type == ReservationPaymentType.DEPOSIT
                    || type == ReservationPaymentType.BALANCE)
                    && (status == ReservationPaymentStatus.SUCCEEDED
                    || status == ReservationPaymentStatus.REFUNDED)) {

                totalCapturedAmount =
                        totalCapturedAmount.add(amount);
            }

            if (type == ReservationPaymentType.REFUND
                    && status == ReservationPaymentStatus.SUCCEEDED) {

                totalRefundedAmount =
                        totalRefundedAmount.add(amount);
            }

            if (type == ReservationPaymentType.REFUND
                    && (status == ReservationPaymentStatus.PENDING
                    || status == ReservationPaymentStatus.PROCESSING)) {

                pendingRefundAmount =
                        pendingRefundAmount.add(amount);
            }
        }

        BigDecimal netPaidAmount = totalCapturedAmount
                .subtract(totalRefundedAmount)
                .max(BigDecimal.ZERO);

        BigDecimal balanceDue = totalCharge
                .subtract(netPaidAmount)
                .max(BigDecimal.ZERO);

        BigDecimal overpaidAmount = netPaidAmount
                .subtract(totalCharge)
                .max(BigDecimal.ZERO);

        return new GetCheckoutBalanceResponse(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                reservation.getStatus(),
                reservation.getCurrency(),
                roomAmount,
                folioTotal,
                totalCharge,
                totalCapturedAmount,
                totalRefundedAmount,
                pendingRefundAmount,
                netPaidAmount,
                balanceDue,
                overpaidAmount
        );
    }

    private void validateReservationProperty(
            Reservation reservation,
            Long propertyId
    ) {

        if (reservation.getProperty() == null
                || !propertyId.equals(
                        reservation.getProperty().getPropertyId()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reservation not found"
            );
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
