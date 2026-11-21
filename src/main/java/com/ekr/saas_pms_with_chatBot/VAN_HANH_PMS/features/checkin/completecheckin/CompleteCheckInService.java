package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.completecheckin;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoom;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRoomRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.CheckInAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompleteCheckInService {

    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final RoomRepository roomRepository;
    private final CheckInAccessService accessService;

    public CompleteCheckInService(
            ReservationRepository reservationRepository,
            ReservationRoomRepository reservationRoomRepository,
            RoomRepository roomRepository,
            CheckInAccessService accessService
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.roomRepository = roomRepository;
        this.accessService = accessService;
    }

    @Transactional
    public CompleteCheckInResponse completeCheckIn(
            Long propertyId,
            String reservationCode
    ) {

        // 1. Kiểm tra quyền thực hiện Check-in.
        accessService.requireReceptionist(
                propertyId,
                "CHECKIN_EXECUTE"
        );

        // 2. Khóa Reservation để ngăn hai nhân viên
        //    đồng thời hoàn tất Check-in cùng một Reservation.
        Reservation reservation = reservationRepository
                .findByReservationCodeForUpdate(reservationCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reservation not found"
                ));

        // 3. Xác minh Reservation thuộc Property của nhân viên.
        if (!reservation.getProperty()
                .getPropertyId()
                .equals(propertyId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reservation not found in this Property"
            );
        }

        // 4. Chỉ Reservation CONFIRMED mới được Check-in.
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation must be CONFIRMED before Check-in"
            );
        }

        // 5. Lấy danh sách Room đã gắn với Reservation.
        List<ReservationRoom> reservationRooms =
                reservationRoomRepository.findAllByReservationId(
                        reservation.getReservationId()
                );

        if (reservationRooms.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation has no assigned rooms"
            );
        }

        // 6. Sắp xếp roomId tăng dần để thống nhất thứ tự khóa.
        List<Long> roomIds = reservationRooms.stream()
                .map(ReservationRoom::getRoomId)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        // 7. Khóa từng Room và kiểm tra thuộc đúng Property.
        Map<Long, Room> lockedRooms = new LinkedHashMap<>();

        for (Long roomId : roomIds) {

            Room room = roomRepository
                    .findByRoomIdAndPropertyIdForUpdate(
                            roomId,
                            propertyId
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "A reserved room does not belong to this Property"
                    ));

            lockedRooms.put(roomId, room);
        }

        // 8. Kiểm tra TẤT CẢ Room phải READY.
        //    Chưa cập nhật bất kỳ dữ liệu nào ở bước này.
        for (Room room : lockedRooms.values()) {

            String currentStatus = room.getStatus() == null
                    ? ""
                    : room.getStatus().trim();

            if (!"READY".equalsIgnoreCase(currentStatus)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Room " + room.getRoomNumber()
                                + " is not READY. Current status: "
                                + currentStatus
                );
            }
        }

        // 9. Tất cả phòng hợp lệ:
        //    cập nhật toàn bộ Room thành OCCUPIED.
        for (Room room : lockedRooms.values()) {
            room.setStatus("OCCUPIED");
        }

        // 10. Cập nhật Reservation thành CHECKED_IN.
        reservation.setStatus(ReservationStatus.CHECKED_IN);

        // 11. Tạo response.
        List<CompleteCheckInResponse.CheckedInRoom> checkedInRooms =
                new ArrayList<>();

        for (Room room : lockedRooms.values()) {

            checkedInRooms.add(
                    new CompleteCheckInResponse.CheckedInRoom(
                            room.getRoomId(),
                            room.getRoomNumber(),
                            room.getFloor(),
                            room.getStatus()
                    )
            );
        }

        return new CompleteCheckInResponse(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                reservation.getStatus().name(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                checkedInRooms
        );
    }
}