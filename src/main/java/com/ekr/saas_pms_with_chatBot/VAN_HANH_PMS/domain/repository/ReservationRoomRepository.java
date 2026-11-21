package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoom;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoom.ReservationRoomId;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRoomRepository
        extends JpaRepository<ReservationRoom, ReservationRoomId> {

    List<ReservationRoom> findAllByReservationId(Long reservationId);

    List<ReservationRoom> findAllByRoomId(Long roomId);

    @Query("""
            SELECT rr
            FROM ReservationRoom rr
            JOIN FETCH rr.reservation reservation
            JOIN FETCH rr.room room
            JOIN FETCH room.roomType roomType
            WHERE roomType.property.propertyId = :propertyId

              AND reservation.status IN :statuses

              AND reservation.checkInDate <= :date
              AND reservation.checkOutDate >= :date

            ORDER BY
                room.floor ASC,
                room.roomNumber ASC
            """)
    List<ReservationRoom> findHousekeepingSchedule(
            @Param("propertyId") Long propertyId,
            @Param("date") LocalDate date,
            @Param("statuses") List<ReservationStatus> statuses
    );
}