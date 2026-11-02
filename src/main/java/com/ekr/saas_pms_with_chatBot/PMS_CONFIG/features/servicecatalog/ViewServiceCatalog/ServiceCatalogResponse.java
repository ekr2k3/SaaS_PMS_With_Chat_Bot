
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.servicecatalog.ViewServiceCatalog;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.ServiceCatalog;

public record ServiceCatalogResponse(
        Long serviceId,
        String name,
        String serviceType,
        String description
) {
    public static ServiceCatalogResponse from(ServiceCatalog service) {
        return new ServiceCatalogResponse(
                service.getServiceId(),
                service.getName(),
                service.getServiceType(),
                service.getDescription()
        );
    }
}