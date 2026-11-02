
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.seeder;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.ServiceCatalog;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.ServiceCatalogRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ServiceCatalogSeeder implements ApplicationRunner {

    private static final String ACTIVE = "ACTIVE";

    private final ServiceCatalogRepository serviceCatalogRepository;

    public ServiceCatalogSeeder(
            ServiceCatalogRepository serviceCatalogRepository
    ) {
        this.serviceCatalogRepository = serviceCatalogRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (ServiceCatalog service : defaultServices()) {
            boolean exists = serviceCatalogRepository
                    .existsByServiceType(service.getServiceType());

            if (!exists) {
                serviceCatalogRepository.save(service);
            }
        }
    }

    private List<ServiceCatalog> defaultServices() {
        return List.of(
                service(
                        "Dịch vụ dọn phòng",
                        "HOUSEKEEPING",
                        "Dịch vụ dọn phòng",
                        ACTIVE
                ),
                service(
                        "Dịch vụ F&B",
                        "FNB",
                        "Dịch vụ đồ ăn và thức uống",
                        ACTIVE
                )
        );
    }

    private ServiceCatalog service(
            String name,
            String serviceType,
            String description,
            String status
    ) {
        return ServiceCatalog.builder()
                .name(name)
                .serviceType(serviceType)
                .description(description)
                .status(status)
                .build();
    }
}