package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationPaymentRepository
        extends JpaRepository<ReservationPayment, Long> {

    List<ReservationPayment>
    findAllByReservation_ReservationIdOrderByReservationPaymentIdDesc(
            Long reservationId
    );

    Optional<ReservationPayment> findByCheckoutSessionId(
            String checkoutSessionId
    );

    Optional<ReservationPayment> findByPaymentIntentId(
            String paymentIntentId
    );

    boolean existsByTransactionId(String transactionId);

    Optional<ReservationPayment> findByGatewayRefundId(String gatewayRefundId);

    List<ReservationPayment> findAllByTransactionIdAndPaymentType(
            String transactionId,
            com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType paymentType
    );

    /**
     * Khóa toàn bộ Payment thuộc Reservation.
     *
     * Sắp xếp theo ID tăng dần để các transaction
     * lấy lock theo cùng một thứ tự.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT payment
            FROM ReservationPayment payment
            WHERE payment.reservation.reservationId = :reservationId
            ORDER BY payment.reservationPaymentId ASC
            """)
    List<ReservationPayment> findAllByReservationIdForUpdate(
            @Param("reservationId") Long reservationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT payment
            FROM ReservationPayment payment
            JOIN FETCH payment.reservation
            WHERE payment.reservationPaymentId = :paymentId
            """)
    Optional<ReservationPayment> findByIdForUpdate(
            @Param("paymentId") Long paymentId
    );
}