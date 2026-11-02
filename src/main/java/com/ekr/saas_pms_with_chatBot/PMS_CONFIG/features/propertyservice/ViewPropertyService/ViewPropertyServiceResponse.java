
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.propertyservice.ViewPropertyService;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PropertyService;

import java.math.BigDecimal;

public record ViewPropertyServiceResponse(
        Long propertyServiceId,
        Long propertyId,
        Long serviceId,
        String name,
        String serviceType,
        String description,
        BigDecimal price,
        String status,
        String catalogStatus
) {

    public static ViewPropertyServiceResponse from(
            PropertyService propertyService
    ) {
        return new ViewPropertyServiceResponse(
                propertyService.getPropertyServiceId(),
                propertyService.getProperty().getPropertyId(),
                propertyService.getService().getServiceId(),
                propertyService.getService().getName(),
                propertyService.getService().getServiceType(),
                propertyService.getDescription(),
                propertyService.getPrice(),
                propertyService.getStatus(),
                propertyService.getService().getStatus()
        );
    }
}