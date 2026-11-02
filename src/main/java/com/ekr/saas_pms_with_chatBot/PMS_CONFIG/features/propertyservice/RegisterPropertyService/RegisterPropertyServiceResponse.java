
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.propertyservice.RegisterPropertyService;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PropertyService;

public record RegisterPropertyServiceResponse(
        Long propertyServiceId,
        Long propertyId,
        Long serviceId,
        String serviceType,
        String status,
        String message
) {

    public static RegisterPropertyServiceResponse from(
            PropertyService propertyService,
            String message
    ) {
        return new RegisterPropertyServiceResponse(
                propertyService.getPropertyServiceId(),
                propertyService.getProperty().getPropertyId(),
                propertyService.getService().getServiceId(),
                propertyService.getService().getServiceType(),
                propertyService.getStatus(),
                message
        );
    }
}