
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.ServiceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceCatalogRepository
        extends JpaRepository<ServiceCatalog, Long> {

    boolean existsByServiceType(String serviceType);

    Optional<ServiceCatalog> findByServiceType(String serviceType);

    List<ServiceCatalog> findAllByStatusOrderByServiceIdAsc(
            String status
    );
}