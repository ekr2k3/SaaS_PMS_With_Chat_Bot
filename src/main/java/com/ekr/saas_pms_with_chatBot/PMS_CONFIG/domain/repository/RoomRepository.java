package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    /**
     * Lấy toàn bộ Room thuộc một Property.
     *
     * Quan hệ truy vấn:
     * Room -> RoomType -> Property
     */
    List<Room> findAllByRoomType_Property_PropertyIdOrderByRoomIdAsc(
            Long propertyId
    );

    /**
     * Tìm Room theo ID và xác minh Room thuộc Property được chỉ định.
     */
    Optional<Room> findByRoomIdAndRoomType_Property_PropertyId(
            Long roomId,
            Long propertyId
    );

    /**
     * Đếm số Room thuộc một Room Type.
     */
    long countByRoomType_RoomTypeId(Long roomTypeId);
}