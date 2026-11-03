package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.expiration;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationPaymentRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpirePendingReservationsService {

    private final ReservationRepository reservationRepository;

    private final ReservationPaymentRepository paymentRepository;

    /**
     * Xử lý tất cả Reservation đã hết thời gian giữ phòng.
     *
     * Toàn bộ quá trình chạy trong một transaction:
     * - Tìm các Reservation có khả năng hết hạn.
     * - Khóa Payment trước.
     * - Khóa Reservation sau.
     * - Kiểm tra lại trạng thái và thời hạn.
     * - Cập nhật trạng thái.
     */
    @Transactional
    public int expirePendingReservations() {

        LocalDateTime now = LocalDateTime.now();

        List<Reservation> candidates =
                reservationRepository
                        .findAllByStatusAndPaymentExpiresAtLessThanEqual(
                                ReservationStatus.PENDING_PAYMENT,
                                now
                        );

        if (candidates.isEmpty()) {
            return 0;
        }

        int expiredCount = 0;

        for (Reservation candidate : candidates) {

            boolean expired = expireReservation(
                    candidate.getReservationId(),
                    candidate.getReservationCode(),
                    now
            );

            if (expired) {
                expiredCount++;
            }
        }

        return expiredCount;
    }

    /**
     * Xử lý một Reservation.
     *
     * Thứ tự lock:
     * 1. ReservationPayment.
     * 2. Reservation.
     *
     * Thứ tự này cần thống nhất với Stripe Webhook.
     */
    private boolean expireReservation(
            Long reservationId,
            String reservationCode,
            LocalDateTime now
    ) {

        // 1. Khóa toàn bộ Payment theo ID tăng dần.
        List<ReservationPayment> payments =
                paymentRepository.findAllByReservationIdForUpdate(
                        reservationId
                );

        // 2. Khóa Reservation sau khi đã khóa Payment.
        Reservation reservation = reservationRepository
                .findByReservationCodeForUpdate(reservationCode)
                .orElse(null);

        if (reservation == null) {
            return false;
        }

        // 3. Kiểm tra lại trạng thái sau khi lấy lock.
        if (reservation.getStatus()
                != ReservationStatus.PENDING_PAYMENT) {
            return false;
        }

        // 4. Kiểm tra lại thời hạn.
        LocalDateTime paymentExpiresAt =
                reservation.getPaymentExpiresAt();

        if (paymentExpiresAt == null
                || paymentExpiresAt.isAfter(now)) {
            return false;
        }

        // 5. Không expire Reservation nếu đã có Payment thành công.
        boolean hasSuccessfulPayment = payments.stream()
                .anyMatch(payment ->
                        payment.getStatus()
                                == ReservationPaymentStatus.SUCCEEDED
                );

        if (hasSuccessfulPayment) {

            log.warn(
                    "Reservation {} đã hết hạn nhưng có Payment SUCCEEDED. "
                            + "Bỏ qua để chờ đồng bộ Webhook.",
                    reservationCode
            );

            return false;
        }

        // 6. Đóng các Payment Attempt chưa hoàn tất.
        for (ReservationPayment payment : payments) {

            ReservationPaymentStatus status = payment.getStatus();

            if (status == ReservationPaymentStatus.PENDING
                    || status == ReservationPaymentStatus.PROCESSING) {

                payment.setStatus(
                        ReservationPaymentStatus.EXPIRED
                );
            }
        }

        // 7. Expire Reservation.
        reservation.setStatus(
                ReservationStatus.EXPIRED
        );

        log.info(
                "Reservation {} đã chuyển sang EXPIRED.",
                reservationCode
        );

        return true;
    }
}