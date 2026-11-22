package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Query("""
            SELECT COUNT(r)
            FROM Reservation r
            WHERE r.property.propertyId = :propertyId
              AND r.createdAt >= :from
              AND r.createdAt < :to
              AND r.status IN :statuses
            """)
    long countSuccessfulReservationsCreatedInDate(
            @Param("propertyId") Long propertyId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("statuses") List<ReservationStatus> statuses
    );

    @Query("""
            SELECT COALESCE(SUM(r.depositAmount), 0)
            FROM Reservation r
            WHERE r.property.propertyId = :propertyId
              AND r.createdAt >= :from
              AND r.createdAt < :to
            """)
    BigDecimal sumDepositAmountCreatedInDate(
            @Param("propertyId") Long propertyId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT COALESCE(SUM(r.guestCount), 0)
            FROM Reservation r
            WHERE r.property.propertyId = :propertyId
              AND r.status = :checkedInStatus
              AND r.checkInDate <= :businessDate
              AND r.checkOutDate > :businessDate
            """)
    Long sumCheckedInGuestCount(
            @Param("propertyId") Long propertyId,
            @Param("businessDate") LocalDate businessDate,
            @Param("checkedInStatus") ReservationStatus checkedInStatus
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
