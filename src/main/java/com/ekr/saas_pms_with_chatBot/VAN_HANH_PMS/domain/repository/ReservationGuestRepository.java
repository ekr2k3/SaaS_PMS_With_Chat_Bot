package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest.ReservationGuestId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationGuestRepository
        extends JpaRepository<ReservationGuest, ReservationGuestId> {

    List<ReservationGuest> findAllByReservationId(Long reservationId);

    List<ReservationGuest> findAllByGuestId(Long guestId);
}
