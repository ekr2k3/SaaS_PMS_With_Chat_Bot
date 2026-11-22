package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoomRepository extends JpaRepository<Room, Long> {

    /**
     * Lấy toàn bộ Room thuộc một Property.
     *
     * Quan hệ:
     * Room -> RoomType -> Property
     */
    List<Room> findAllByRoomType_Property_PropertyIdOrderByRoomIdAsc(
            Long propertyId
    );

    /**
     * Tìm Room theo ID và xác minh Room thuộc Property.
     */
    Optional<Room> findByRoomIdAndRoomType_Property_PropertyId(
            Long roomId,
            Long propertyId
    );

    /**
     * Đếm số Room thuộc một Room Type.
     */
    long countByRoomType_RoomTypeId(Long roomTypeId);

    /**
     * Tìm các Room còn khả dụng trong khoảng ngày yêu cầu.
     *
     * Điều kiện Room:
     * 1. Thuộc Property.
     * 2. Room Type ACTIVE.
     * 3. Room.isAvailable = true.
     * 4. Room.status = READY.
     * 5. Có ít nhất một Room Rate ACTIVE.
     * 6. Không có Reservation chiếm dụng trong khoảng ngày.
     *
     * Reservation chặn phòng:
     * - CONFIRMED
     * - CHECKED_IN
     * - PENDING_PAYMENT còn thời hạn giữ phòng.
     *
     * PENDING_PAYMENT có paymentExpiresAt null được xem
     * là vẫn đang giữ phòng.
     */
    @Query("""
            SELECT room
            FROM Room room
            WHERE room.roomType.property.propertyId = :propertyId

              AND room.roomType.status = :activeRoomTypeStatus

              AND room.isAvailable = true

              AND room.status = 'READY'

              AND (
                    :roomTypeId IS NULL
                    OR room.roomType.roomTypeId = :roomTypeId
              )

              AND EXISTS (
                    SELECT rate.roomRateId
                    FROM RoomRate rate
                    WHERE rate.roomType.roomTypeId =
                          room.roomType.roomTypeId
                      AND rate.status = :activeRateStatus
              )

              AND NOT EXISTS (
                    SELECT reservationRoom.reservationId
                    FROM ReservationRoom reservationRoom
                    JOIN reservationRoom.reservation reservation
                    WHERE reservationRoom.room.roomId = room.roomId

                      AND reservation.checkInDate < :checkOutDate
                      AND reservation.checkOutDate > :checkInDate

                      AND (
                            reservation.status IN :blockingStatuses

                            OR (
                                reservation.status = :pendingPaymentStatus
                                AND (
                                    reservation.paymentExpiresAt IS NULL
                                    OR reservation.paymentExpiresAt > :now
                                )
                            )
                      )
              )

            ORDER BY
                room.roomType.roomTypeId ASC,
                room.roomId ASC
            """)
    List<Room> findAvailableRooms(
            @Param("propertyId") Long propertyId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("now") LocalDateTime now,
            @Param("activeRoomTypeStatus") String activeRoomTypeStatus,
            @Param("activeRateStatus") String activeRateStatus,
            @Param("blockingStatuses") Set<ReservationStatus> blockingStatuses,
            @Param("pendingPaymentStatus") ReservationStatus pendingPaymentStatus
    );

    /**
     * Đếm phòng đang được Reservation CHECKED_IN chiếm dụng.
     *
     * Không sử dụng Room.status để xác định phòng đang có khách.
     */
    @Query("""
            SELECT COUNT(DISTINCT rr.roomId)
            FROM ReservationRoom rr
            JOIN rr.reservation reservation
            JOIN rr.room room
            WHERE room.roomType.property.propertyId = :propertyId
              AND reservation.status = :checkedInStatus
              AND reservation.checkInDate <= :businessDate
              AND reservation.checkOutDate > :businessDate
            """)
    long countOccupiedRooms(
            @Param("propertyId") Long propertyId,
            @Param("businessDate") LocalDate businessDate,
            @Param("checkedInStatus") ReservationStatus checkedInStatus
    );

    /**
     * Đếm phòng có isAvailable = true và không bị Reservation
     * chiếm dụng trong ngày nghiệp vụ.
     *
     * Không kiểm tra Room.status = READY/DIRTY.
     */
    @Query("""
            SELECT COUNT(room)
            FROM Room room
            WHERE room.roomType.property.propertyId = :propertyId
              AND room.isAvailable = true
              AND NOT EXISTS (
                  SELECT rr.roomId
                  FROM ReservationRoom rr
                  JOIN rr.reservation reservation
                  WHERE rr.roomId = room.roomId
                    AND reservation.checkInDate <= :businessDate
                    AND reservation.checkOutDate > :businessDate
                    AND (
                        reservation.status IN :blockingStatuses
                        OR (
                            reservation.status = :pendingPaymentStatus
                            AND (
                                reservation.paymentExpiresAt IS NULL
                                OR reservation.paymentExpiresAt > :now
                            )
                        )
                    )
              )
            """)
    long countAvailableRoomsForBusinessDate(
            @Param("propertyId") Long propertyId,
            @Param("businessDate") LocalDate businessDate,
            @Param("now") LocalDateTime now,
            @Param("blockingStatuses") List<ReservationStatus> blockingStatuses,
            @Param("pendingPaymentStatus") ReservationStatus pendingPaymentStatus
    );

    /**
     * Khóa Room trong transaction khi tạo Reservation.
     *
     * Đồng thời xác minh Room thuộc đúng Property.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM Room r
            WHERE r.roomId = :roomId
              AND r.roomType.property.propertyId = :propertyId
            """)
    Optional<Room> findByRoomIdAndPropertyIdForUpdate(
            @Param("roomId") Long roomId,
            @Param("propertyId") Long propertyId
    );
}