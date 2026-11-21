package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReservationCode(String reservationCode);

    boolean existsByReservationCode(String reservationCode);

    List<Reservation> findAllByStatusAndPaymentExpiresAtLessThanEqual(
            ReservationStatus status,
            LocalDateTime expiresAt
    );

    List<Reservation>
    findAllByProperty_PropertyIdAndStatusOrderByCheckInDateAsc(
            Long propertyId,
            ReservationStatus status
    );

    /**
     * Kiểm tra một Room có Reservation trùng khoảng ngày hay không.
     *
     * Khoảng thời gian lưu trú được hiểu là:
     * [checkInDate, checkOutDate)
     *
     * Reservation chặn phòng:
     * - CONFIRMED
     * - CHECKED_IN
     * - PENDING_PAYMENT còn thời hạn giữ phòng
     */
    @Query("""
            SELECT COUNT(rr)
            FROM ReservationRoom rr
            JOIN rr.reservation r
            WHERE rr.roomId = :roomId
              AND r.property.propertyId = :propertyId
              AND r.checkInDate < :checkOutDate
              AND r.checkOutDate > :checkInDate
              AND (
                    r.status IN :blockingStatuses
                    OR (
                        r.status = :pendingStatus
                        AND r.paymentExpiresAt > :now
                    )
              )
            """)
    long countConflictingReservations(
            @Param("roomId") Long roomId,
            @Param("propertyId") Long propertyId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("blockingStatuses")
            List<ReservationStatus> blockingStatuses,
            @Param("pendingStatus") ReservationStatus pendingStatus,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.reservationCode = :reservationCode
            """)
    Optional<Reservation> findByReservationCodeForUpdate(
            @Param("reservationCode") String reservationCode
    );
}
