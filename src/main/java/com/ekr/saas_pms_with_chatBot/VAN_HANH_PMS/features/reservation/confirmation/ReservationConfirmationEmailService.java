package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.confirmation;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.MailService;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Guest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoom;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoomRateSnapshot;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationGuestRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRoomRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRoomRateSnapshotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationConfirmationEmailService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReservationRepository reservationRepository;

    private final ReservationGuestRepository reservationGuestRepository;

    private final ReservationRoomRepository reservationRoomRepository;

    private final ReservationRoomRateSnapshotRepository snapshotRepository;

    private final MailService mailService;

    @Transactional(readOnly = true)
    public void sendConfirmation(Long reservationId) {

        // 1. Lấy Reservation.
        Reservation reservation = reservationRepository
                .findById(reservationId)
                .orElse(null);

        if (reservation == null) {
            log.warn(
                    "Không tìm thấy Reservation ID {} để gửi email.",
                    reservationId
            );
            return;
        }

        // 2. Chỉ gửi email cho Reservation đã xác nhận.
        if (reservation.getStatus()
                != com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus.CONFIRMED) {

            log.warn(
                    "Reservation {} chưa CONFIRMED. Bỏ qua email.",
                    reservation.getReservationCode()
            );
            return;
        }

        // 3. Lấy Guest của Reservation.
        List<ReservationGuest> reservationGuests =
                reservationGuestRepository.findAllByReservationId(
                        reservationId
                );

        if (reservationGuests.isEmpty()) {
            log.warn(
                    "Reservation {} không có Guest.",
                    reservation.getReservationCode()
            );
            return;
        }

        // Slice hiện tại chỉ tạo một Guest chính cho Reservation.
        Guest guest = reservationGuests.get(0).getGuest();

        if (guest == null
                || guest.getEmail() == null
                || guest.getEmail().isBlank()) {

            log.info(
                    "Reservation {} không có email Guest. Bỏ qua gửi email.",
                    reservation.getReservationCode()
            );
            return;
        }

        // 4. Lấy thông tin Property.
        var property = reservation.getProperty();

        // 5. Lấy danh sách phòng.
        List<ReservationRoom> reservationRooms =
                reservationRoomRepository.findAllByReservationId(
                        reservationId
                );

        List<String> roomDetails = reservationRooms.stream()
                .map(reservationRoom -> {

                    var room = reservationRoom.getRoom();

                    String roomTypeName =
                            room.getRoomType() != null
                                    && room.getRoomType().getName() != null
                                    ? room.getRoomType().getName().name()
                                    : "N/A";

                    // Lấy giá snapshot của phòng.
                    List<ReservationRoomRateSnapshot> snapshots =
                            snapshotRepository
                                    .findAllByReservationIdAndRoomIdOrderByStayDateAsc(
                                            reservationId,
                                            reservationRoom.getRoomId()
                                    );

                    String rateDetails = snapshots.stream()
                            .map(snapshot ->
                                    snapshot.getStayDate().format(DATE_FORMAT)
                                            + ": "
                                            + snapshot.getPrice().toPlainString()
                                            + " "
                                            + reservation.getCurrency()
                            )
                            .reduce(
                                    (first, second) ->
                                            first + "\n      " + second
                            )
                            .orElse("Không có thông tin giá");

                    return """
                            - Phòng: %s
                              Loại phòng: %s
                              Giá theo từng đêm:
                              %s
                            """.formatted(
                            room.getRoomNumber(),
                            roomTypeName,
                            rateDetails
                    );
                })
                .toList();

        // 6. Gửi email.
        mailService.sendReservationConfirmationEmail(
                guest.getEmail(),
                guest.getFullName(),
                reservation.getReservationCode(),
                property.getName(),
                property.getAddress(),
                property.getPhone(),
                reservation.getCheckInDate().format(DATE_FORMAT),
                reservation.getCheckOutDate().format(DATE_FORMAT),
                reservation.getGuestCount(),
                reservation.getTotalAmount().toPlainString(),
                reservation.getDepositAmount().toPlainString(),
                reservation.getCurrency(),
                roomDetails
        );

        log.info(
                "Đã gửi email xác nhận Reservation {} tới {}.",
                reservation.getReservationCode(),
                guest.getEmail()
        );
    }
}