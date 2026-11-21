package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.get;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Guest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoom;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoomRateSnapshot;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationGuestRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationPaymentRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRoomRateSnapshotRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRoomRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class GetReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationGuestRepository reservationGuestRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final ReservationRoomRateSnapshotRepository snapshotRepository;
    private final ReservationPaymentRepository paymentRepository;

    private final GetReservationValidator validator;

    public GetReservationService(
            ReservationRepository reservationRepository,
            ReservationGuestRepository reservationGuestRepository,
            ReservationRoomRepository reservationRoomRepository,
            ReservationRoomRateSnapshotRepository snapshotRepository,
            ReservationPaymentRepository paymentRepository,
            GetReservationValidator validator
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationGuestRepository = reservationGuestRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.snapshotRepository = snapshotRepository;
        this.paymentRepository = paymentRepository;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public GetReservationResponse getReservation(
            String reservationCode,
            String email,
            String phone
    ) {

        // 1. Validate query parameters.
        validator.validate(reservationCode, email, phone);

        String normalizedCode = reservationCode.trim();
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhone(phone);

        // 2. Find Reservation.
        Reservation reservation = reservationRepository
                .findByReservationCode(normalizedCode)
                .orElseThrow(this::reservationNotFound);

        // 3. Verify Guest contact information.
        Guest verifiedGuest = verifyGuest(
                reservation,
                normalizedEmail,
                normalizedPhone
        );

        // 4. Get Rooms and their historical prices.
        List<GetReservationResponse.RoomDetail> roomDetails =
                getRoomDetails(reservation);

        // 5. Get payment history.
        List<GetReservationResponse.PaymentDetail> paymentDetails =
                getPaymentDetails(reservation);

        // 6. Check whether the payment hold has expired.
        LocalDateTime now = LocalDateTime.now();

        boolean paymentHoldExpired =
                reservation.getStatus() == ReservationStatus.PENDING_PAYMENT
                        && reservation.getPaymentExpiresAt() != null
                        && !reservation.getPaymentExpiresAt().isAfter(now);

        int numberOfNights = Math.toIntExact(
                ChronoUnit.DAYS.between(
                        reservation.getCheckInDate(),
                        reservation.getCheckOutDate()
                )
        );

        return new GetReservationResponse(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                reservation.getStatus(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                numberOfNights,
                reservation.getGuestCount(),
                reservation.getTotalAmount(),
                reservation.getDepositAmount(),
                reservation.getCurrency(),
                reservation.getCreatedAt(),
                reservation.getPaymentExpiresAt(),
                paymentHoldExpired,
                toGuestDetail(verifiedGuest),
                roomDetails,
                paymentDetails
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
                            || (guest.getEmail() != null
                            && guest.getEmail()
                            .trim()
                            .equalsIgnoreCase(email));

            boolean phoneMatches =
                    phone == null
                            || (guest.getPhone() != null
                            && normalizePhone(guest.getPhone())
                            .equals(phone));

            if (emailMatches && phoneMatches) {
                return guest;
            }
        }

        // Same response whether Reservation is missing
        // or contact information does not match.
        throw reservationNotFound();
    }

    private List<GetReservationResponse.RoomDetail> getRoomDetails(
            Reservation reservation
    ) {

        List<ReservationRoom> reservationRooms =
                reservationRoomRepository.findAllByReservationId(
                        reservation.getReservationId()
                );

        List<GetReservationResponse.RoomDetail> result =
                new ArrayList<>();

        for (ReservationRoom reservationRoom : reservationRooms) {

            if (reservationRoom.getRoom() == null) {
                throw new IllegalStateException(
                        "Reservation Room has no associated Room"
                );
            }

            List<ReservationRoomRateSnapshot> snapshots =
                    snapshotRepository
                            .findAllByReservationIdAndRoomIdOrderByStayDateAsc(
                                    reservation.getReservationId(),
                                    reservationRoom.getRoomId()
                            );

            List<GetReservationResponse.NightlyPriceDetail>
                    nightlyPrices = snapshots.stream()
                    .map(snapshot ->
                            new GetReservationResponse.NightlyPriceDetail(
                                    snapshot.getStayDate(),
                                    snapshot.getRoomRateId(),
                                    snapshot.getRateName(),
                                    snapshot.getPrice()
                            )
                    )
                    .toList();

            BigDecimal roomTotal = snapshots.stream()
                    .map(ReservationRoomRateSnapshot::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.add(
                    new GetReservationResponse.RoomDetail(
                            reservationRoom.getRoomId(),
                            reservationRoom.getRoom().getRoomNumber(),
                            reservationRoom.getRoom().getFloor(),
                            reservationRoom.getRoom()
                                    .getRoomType()
                                    .getName()
                                    .name(),
                            nightlyPrices,
                            roomTotal
                    )
            );
        }

        return result;
    }

    private List<GetReservationResponse.PaymentDetail> getPaymentDetails(
            Reservation reservation
    ) {

        List<ReservationPayment> payments =
                paymentRepository
                        .findAllByReservation_ReservationIdOrderByReservationPaymentIdDesc(
                                reservation.getReservationId()
                        );

        return payments.stream()
                .map(payment ->
                        new GetReservationResponse.PaymentDetail(
                                payment.getReservationPaymentId(),
                                payment.getPaymentType(),
                                payment.getAmount(),
                                payment.getCurrency() != null
                                        ? payment.getCurrency()
                                        : reservation.getCurrency(),
                                payment.getStatus(),
                                payment.getGatewayRefundId(),
                                payment.getPaidAt()
                        )
                )
                .toList();
    }

    private GetReservationResponse.GuestDetail toGuestDetail(
            Guest guest
    ) {

        return new GetReservationResponse.GuestDetail(
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
