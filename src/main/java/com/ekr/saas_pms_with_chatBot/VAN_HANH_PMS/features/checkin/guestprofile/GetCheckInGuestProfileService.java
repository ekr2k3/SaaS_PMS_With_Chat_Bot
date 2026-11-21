package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.guestprofile;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Guest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationGuestRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.CheckInAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GetCheckInGuestProfileService {

    private final ReservationRepository reservationRepository;
    private final ReservationGuestRepository reservationGuestRepository;
    private final CheckInAccessService accessService;

    public GetCheckInGuestProfileService(
            ReservationRepository reservationRepository,
            ReservationGuestRepository reservationGuestRepository,
            CheckInAccessService accessService
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationGuestRepository = reservationGuestRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public GetCheckInGuestProfileResponse getGuestProfiles(
            Long propertyId,
            String reservationCode
    ) {

        // 1. Kiểm tra quyền Receptionist.
        accessService.requireReceptionist(
                propertyId,
                "GUEST_VIEW"
        );

        // 2. Tìm Reservation.
        Reservation reservation = reservationRepository
                .findByReservationCode(reservationCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reservation not found"
                ));

        // 3. Xác minh Reservation thuộc Property hiện tại.
        if (!reservation.getProperty()
                .getPropertyId()
                .equals(propertyId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reservation not found in this Property"
            );
        }

        // 4. Lấy các Guest đã liên kết với Reservation.
        List<ReservationGuest> reservationGuests =
                reservationGuestRepository.findAllByReservationId(
                        reservation.getReservationId()
                );

        // 5. Chuyển Entity sang Response.
        List<GetCheckInGuestProfileResponse.GuestProfile> guests =
                reservationGuests.stream()
                        .map(ReservationGuest::getGuest)
                        .map(this::toGuestProfile)
                        .toList();

        return new GetCheckInGuestProfileResponse(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                reservation.getGuestCount(),
                guests
        );
    }

    private GetCheckInGuestProfileResponse.GuestProfile toGuestProfile(
            Guest guest
    ) {
        return new GetCheckInGuestProfileResponse.GuestProfile(
                guest.getGuestId(),
                guest.getFullName(),
                guest.getPhone(),
                guest.getEmail(),
                guest.getIdentityNumber(),
                guest.getIdentityType(),
                guest.getDateOfBirth(),
                guest.getGender(),
                guest.getNationality(),
                guest.getAddress()
        );
    }
}