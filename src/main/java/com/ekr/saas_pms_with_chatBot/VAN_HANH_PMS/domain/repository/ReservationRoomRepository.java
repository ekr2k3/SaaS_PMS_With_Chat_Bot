package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoom;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoom.ReservationRoomId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRoomRepository
        extends JpaRepository<ReservationRoom, ReservationRoomId> {

    List<ReservationRoom> findAllByReservationId(Long reservationId);

    List<ReservationRoom> findAllByRoomId(Long roomId);
}
