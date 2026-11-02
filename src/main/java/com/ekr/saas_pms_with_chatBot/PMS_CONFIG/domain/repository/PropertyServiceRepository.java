
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PropertyService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyServiceRepository
        extends JpaRepository<PropertyService, Long> {

    Optional<PropertyService>
    findByProperty_PropertyIdAndService_ServiceId(
            Long propertyId,
            Long serviceId
    );


    List<PropertyService>
    findAllByProperty_PropertyIdOrderByPropertyServiceIdAsc(
            Long propertyId
    );

    Optional<PropertyService>
    findByPropertyServiceIdAndProperty_PropertyId(
            Long propertyServiceId,
            Long propertyId
    );
}