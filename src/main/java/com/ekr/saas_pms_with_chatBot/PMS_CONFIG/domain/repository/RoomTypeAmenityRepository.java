
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomTypeAmenity;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomTypeAmenity.RoomTypeAmenityId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomTypeAmenityRepository
        extends JpaRepository<RoomTypeAmenity, RoomTypeAmenityId> {

    List<RoomTypeAmenity> findAllByRoomTypeId(Long roomTypeId);

    void deleteAllByRoomTypeId(Long roomTypeId);
}