package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Folio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolioRepository
        extends JpaRepository<Folio, Long> {

    List<Folio>
    findAllByReservation_ReservationIdOrderByFolioIdAsc(
            Long reservationId
    );

    List<Folio>
    findAllByGuest_GuestIdOrderByFolioIdAsc(
            Long guestId
    );
}