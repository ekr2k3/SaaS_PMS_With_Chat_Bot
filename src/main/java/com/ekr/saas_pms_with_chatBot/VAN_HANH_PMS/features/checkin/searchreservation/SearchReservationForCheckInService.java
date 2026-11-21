package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.searchreservation;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Guest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoom;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationGuestRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRoomRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.CheckInAccessService;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.get.GetReservationValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class SearchReservationForCheckInService {

    private static final String REQUIRED_PERMISSION =
            "RESERVATION_VIEW";

    private final CheckInAccessService accessService;

    private final ReservationRepository reservationRepository;

    private final ReservationGuestRepository reservationGuestRepository;

    private final ReservationRoomRepository reservationRoomRepository;

    private final GetReservationValidator validator;

    public SearchReservationForCheckInService(
            CheckInAccessService accessService,
            ReservationRepository reservationRepository,
            ReservationGuestRepository reservationGuestRepository,
            ReservationRoomRepository reservationRoomRepository,
            GetReservationValidator validator
    ) {
        this.accessService = accessService;
        this.reservationRepository = reservationRepository;
        this.reservationGuestRepository = reservationGuestRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public SearchReservationForCheckInResponse search(
            Long propertyId,
            String reservationCode,
            String email,
            String phone
    ) {

        // 1. Kiểm tra quyền nhân viên.
        accessService.requireReceptionist(
                propertyId,
                REQUIRED_PERMISSION
        );

        // 2. Tái sử dụng Validator của F11 hiện tại.
        validator.validate(
                reservationCode,
                email,
                phone
        );

        String normalizedCode = reservationCode.trim();

        String normalizedEmail = normalizeEmail(email);

        String normalizedPhone = normalizePhone(phone);

        // 3. Tìm Reservation.
        Reservation reservation = reservationRepository
                .findByReservationCode(normalizedCode)
                .orElseThrow(this::reservationNotFound);

        // 4. Kiểm tra Reservation thuộc Property.
        if (reservation.getProperty() == null
                || !propertyId.equals(
                        reservation.getProperty().getPropertyId()
                )) {

            throw reservationNotFound();
        }

        // 5. Xác minh Guest.
        Guest verifiedGuest = verifyGuest(
                reservation,
                normalizedEmail,
                normalizedPhone
        );

        // 6. Lấy danh sách Room.
        List<SearchReservationForCheckInResponse.RoomDetail>
                roomDetails = getRoomDetails(
                        reservation,
                        propertyId
                );

        // 7. Trả kết quả.
        return new SearchReservationForCheckInResponse(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                reservation.getStatus(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getGuestCount(),
                toGuestDetail(verifiedGuest),
                roomDetails
        );
    }

    private Guest verifyGuest(
            Reservation reservation,
            String email,
            String phone
    ) {

        List<ReservationGuest> reservationGuests =
                reservationGuestRepository.findAllByReservationId(
                        reservation.getReservationId()
                );

        for (ReservationGuest reservationGuest : reservationGuests) {

            Guest guest = reservationGuest.getGuest();

            if (guest == null) {
                continue;
            }

            boolean emailMatches =
                    email == null
                            || (
                                    guest.getEmail() != null
                                    && guest.getEmail()
                                            .trim()
                                            .equalsIgnoreCase(email)
                            );

            boolean phoneMatches =
                    phone == null
                            || (
                                    guest.getPhone() != null
                                    && normalizePhone(guest.getPhone())
                                            .equals(phone)
                            );

            if (emailMatches && phoneMatches) {
                return guest;
            }
        }

        throw reservationNotFound();
    }

    private List<SearchReservationForCheckInResponse.RoomDetail>
    getRoomDetails(
            Reservation reservation,
            Long propertyId
    ) {

        List<ReservationRoom> reservationRooms =
                reservationRoomRepository.findAllByReservationId(
                        reservation.getReservationId()
                );

        List<SearchReservationForCheckInResponse.RoomDetail> result =
                new ArrayList<>();

        for (ReservationRoom reservationRoom : reservationRooms) {

            Room room = reservationRoom.getRoom();

            if (room == null
                    || room.getRoomType() == null
                    || room.getRoomType().getProperty() == null
                    || !propertyId.equals(
                            room.getRoomType()
                                    .getProperty()
                                    .getPropertyId()
                    )) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Reservation contains an invalid Room"
                );
            }

            String roomStatus = room.getStatus() == null
                    ? null
                    : room.getStatus()
                            .trim()
                            .toUpperCase(Locale.ROOT);

            result.add(
                    new SearchReservationForCheckInResponse.RoomDetail(
                            room.getRoomId(),
                            room.getRoomNumber(),
                            room.getFloor(),
                            roomStatus
                    )
            );
        }

        return result;
    }

    private SearchReservationForCheckInResponse.GuestDetail
    toGuestDetail(Guest guest) {

        return new SearchReservationForCheckInResponse.GuestDetail(
                guest.getGuestId(),
                guest.getFullName(),
                guest.getEmail(),
                guest.getPhone(),
                guest.getIdentityNumber(),
                guest.getIdentityType(),
                guest.getDateOfBirth(),
                guest.getGender(),
                guest.getNationality(),
                guest.getAddress()
        );
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {

        if (phone == null || phone.isBlank()) {
            return null;
        }

        return phone.trim();
    }

    private ResponseStatusException reservationNotFound() {

        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Reservation not found or verification information is incorrect"
        );
    }
}
