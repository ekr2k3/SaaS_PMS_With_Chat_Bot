package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository
        extends JpaRepository<Property, Long> {

    long countByTenant_TenantId(Long tenantId);

    List<Property> findAllByTenant_TenantIdOrderByPropertyIdAsc(
            Long tenantId
    );

    Optional<Property> findByPropertyIdAndTenant_TenantId(
            Long propertyId,
            Long tenantId
    );

    @EntityGraph(attributePaths = {"tenant", "tenant.account"})
    Optional<Property> findWithTenantAndAccountByPropertyId(Long propertyId);

    @EntityGraph(attributePaths = {"tenant", "tenant.account"})
    Optional<Property> findWithTenantAndAccountByPropertyIdAndTenant_TenantId(
            Long propertyId,
            Long tenantId
    );
}