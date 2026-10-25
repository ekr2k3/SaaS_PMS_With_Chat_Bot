
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomRate;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.enums.RateName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRateRepository
        extends JpaRepository<RoomRate, Long> {

    List<RoomRate> findAllByRoomType_Property_PropertyIdOrderByRoomRateIdAsc(
            Long propertyId
    );

    Optional<RoomRate> findByRoomRateIdAndRoomType_Property_PropertyId(
            Long roomRateId,
            Long propertyId
    );

    boolean existsByRoomType_RoomTypeIdAndRateName(
            Long roomTypeId,
            RateName rateName
    );

    boolean existsByRoomType_RoomTypeIdAndRateNameAndRoomRateIdNot(
            Long roomTypeId,
            RateName rateName,
            Long roomRateId
    );
}