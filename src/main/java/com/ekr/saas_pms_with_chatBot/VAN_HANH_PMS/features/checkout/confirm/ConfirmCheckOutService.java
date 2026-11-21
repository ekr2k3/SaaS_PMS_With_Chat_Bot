package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.confirm;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomRepository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoom;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRoomRepository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.CheckoutAccessService;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.balance.GetCheckoutBalanceResponse;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.balance.GetCheckoutBalanceService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConfirmCheckOutService {

    private static final String REQUIRED_PERMISSION = "CHECKOUT_EXECUTE";

    private static final String ROOM_OCCUPIED = "OCCUPIED";

    private static final String ROOM_DIRTY = "DIRTY";

    private final CheckoutAccessService accessService;

    private final ReservationRepository reservationRepository;

    private final ReservationRoomRepository reservationRoomRepository;

    private final RoomRepository roomRepository;

    private final GetCheckoutBalanceService balanceService;

    public ConfirmCheckOutService(
            CheckoutAccessService accessService,
            ReservationRepository reservationRepository,
            ReservationRoomRepository reservationRoomRepository,
            RoomRepository roomRepository,
            GetCheckoutBalanceService balanceService
    ) {
        this.accessService = accessService;
        this.reservationRepository = reservationRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.roomRepository = roomRepository;
        this.balanceService = balanceService;
    }

    @Transactional
    public ConfirmCheckOutResponse confirmCheckOut(
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
                .findByReservationCodeForUpdate(
                        reservationCode.trim()
                )
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
                    "Reservation not found in this Property"
            );
        }

        if (reservation.getStatus()
                != ReservationStatus.CHECKED_IN) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation phải ở trạng thái CHECKED_IN "
                            + "mới được Check-out"
            );
        }

        List<ReservationRoom> reservationRooms =
                reservationRoomRepository.findAllByReservationId(
                        reservation.getReservationId()
                );

        if (reservationRooms.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation chưa được gán Room"
            );
        }

        List<Long> roomIds = reservationRooms.stream()
                .map(ReservationRoom::getRoomId)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        Map<Long, Room> lockedRooms = new LinkedHashMap<>();

        for (Long roomId : roomIds) {

            Room room = roomRepository
                    .findByRoomIdAndPropertyIdForUpdate(
                            roomId,
                            propertyId
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Room " + roomId
                                    + " không thuộc Property này"
                    ));

            lockedRooms.put(
                    roomId,
                    room
            );
        }

        for (Room room : lockedRooms.values()) {

            String currentStatus = room.getStatus() == null
                    ? ""
                    : room.getStatus().trim();

            if (!ROOM_OCCUPIED.equalsIgnoreCase(currentStatus)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Room " + room.getRoomNumber()
                                + " không ở trạng thái OCCUPIED. "
                                + "Current status: " + currentStatus
                );
            }
        }

        GetCheckoutBalanceResponse balance =
                balanceService.calculateBalance(reservation);

        BigDecimal balanceDue = balance.balanceDue();

        if (balanceDue == null
                || balanceDue.compareTo(BigDecimal.ZERO) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation chưa thanh toán đủ. "
                            + "Balance Due: " + balanceDue
            );
        }

        List<ConfirmCheckOutResponse.CheckedOutRoom> checkedOutRooms =
                new ArrayList<>();

        for (Room room : lockedRooms.values()) {

            String previousStatus = room.getStatus();
            room.setStatus(ROOM_DIRTY);

            checkedOutRooms.add(
                    new ConfirmCheckOutResponse.CheckedOutRoom(
                            room.getRoomId(),
                            room.getRoomNumber(),
                            previousStatus,
                            room.getStatus()
                    )
            );
        }

        reservation.setStatus(
                ReservationStatus.CHECKED_OUT
        );

        return new ConfirmCheckOutResponse(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                reservation.getStatus().name(),
                reservation.getCurrency(),
                balance.totalCharge(),
                balance.netPaidAmount(),
                balance.balanceDue(),
                checkedOutRooms
        );
    }
}
