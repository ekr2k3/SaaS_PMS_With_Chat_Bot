package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoomRateSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRoomRateSnapshotRepository
        extends JpaRepository<ReservationRoomRateSnapshot, Long> {

    List<ReservationRoomRateSnapshot>
    findAllByReservationIdAndRoomIdOrderByStayDateAsc(
            Long reservationId,
            Long roomId
    );
}
