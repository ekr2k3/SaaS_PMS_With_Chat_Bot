package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.guestfnb.access;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Guest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationGuestRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ResolveGuestReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationGuestRepository reservationGuestRepository;

    public ResolveGuestReservationService(
            ReservationRepository reservationRepository,
            ReservationGuestRepository reservationGuestRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationGuestRepository = reservationGuestRepository;
    }

    public GuestReservationContext resolve(
            String reservationCode,
            String email,
            String phone
    ) {
        Reservation reservation = reservationRepository
                .findByReservationCode(normalizeCode(reservationCode))
                .orElseThrow(this::reservationNotFound);

        return verifyGuest(reservation, email, phone);
    }

    public GuestReservationContext resolveForUpdate(
            String reservationCode,
            String email,
            String phone
    ) {
        Reservation reservation = reservationRepository
                .findByReservationCodeForUpdate(normalizeCode(reservationCode))
                .orElseThrow(this::reservationNotFound);

        return verifyGuest(reservation, email, phone);
    }

    private GuestReservationContext verifyGuest(
            Reservation reservation,
            String email,
            String phone
    ) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhone(phone);

        if (normalizedEmail == null && normalizedPhone == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email hoặc số điện thoại là bắt buộc"
            );
        }

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
                    normalizedEmail == null
                            || (
                            guest.getEmail() != null
                                    && guest.getEmail()
                                    .trim()
                                    .equalsIgnoreCase(normalizedEmail)
                    );

            boolean phoneMatches =
                    normalizedPhone == null
                            || (
                            guest.getPhone() != null
                                    && normalizePhone(guest.getPhone())
                                    .equals(normalizedPhone)
                    );

            if (emailMatches && phoneMatches) {
                return new GuestReservationContext(
                        reservation,
                        guest
                );
            }
        }

        throw reservationNotFound();
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation code là bắt buộc"
            );
        }

        return value.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        return phone.replaceAll("[^0-9]", "");
    }

    private ResponseStatusException reservationNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy Reservation hoặc thông tin Guest không hợp lệ"
        );
    }
}