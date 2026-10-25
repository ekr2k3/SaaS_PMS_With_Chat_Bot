
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomTypeRepository
        extends JpaRepository<RoomType, Long> {

    List<RoomType> findAllByProperty_PropertyIdOrderByRoomTypeIdAsc(
            Long propertyId
    );

    Optional<RoomType> findByRoomTypeIdAndProperty_PropertyId(
            Long roomTypeId,
            Long propertyId
    );

    @Query("""
            select coalesce(sum(rt.numberOfRooms), 0)
            from RoomType rt
            where rt.property.tenant.tenantId = :tenantId
            """)
    long sumNumberOfRoomsByTenantId(
            @Param("tenantId") Long tenantId
    );
}