package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoomRateSnapshot;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRoomRateSnapshotRepository
        extends JpaRepository<ReservationRoomRateSnapshot, Long> {

    List<ReservationRoomRateSnapshot>
    findAllByReservationIdAndRoomIdOrderByStayDateAsc(
            Long reservationId,
            Long roomId
    );

    @Query("""
            SELECT COALESCE(SUM(snapshot.price), 0)
            FROM ReservationRoomRateSnapshot snapshot
            JOIN snapshot.reservationRoom rr
            JOIN rr.reservation reservation
            WHERE reservation.property.propertyId = :propertyId
              AND reservation.createdAt >= :from
              AND reservation.createdAt < :to
              AND reservation.status IN :statuses
            """)
    BigDecimal sumExpectedRoomRevenue(
            @Param("propertyId") Long propertyId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("statuses") List<ReservationStatus> statuses
    );
}
