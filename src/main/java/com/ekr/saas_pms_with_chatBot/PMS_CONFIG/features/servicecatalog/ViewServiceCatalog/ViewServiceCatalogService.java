
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.servicecatalog.ViewServiceCatalog;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.ServiceCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ViewServiceCatalogService {

    private static final String ACTIVE = "ACTIVE";

    private final ServiceCatalogRepository serviceCatalogRepository;

    public ViewServiceCatalogService(
            ServiceCatalogRepository serviceCatalogRepository
    ) {
        this.serviceCatalogRepository = serviceCatalogRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceCatalogResponse> getActiveServices() {
        return serviceCatalogRepository
                .findAllByStatusOrderByServiceIdAsc(ACTIVE)
                .stream()
                .map(ServiceCatalogResponse::from)
                .toList();
    }
}