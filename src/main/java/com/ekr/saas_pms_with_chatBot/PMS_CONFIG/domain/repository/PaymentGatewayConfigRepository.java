
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PaymentGatewayConfig;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentGatewayConfigRepository
        extends JpaRepository<PaymentGatewayConfig, Long> {

    boolean existsByProperty_PropertyId(Long propertyId);

    @EntityGraph(attributePaths = {"property", "gateway"})
    Optional<PaymentGatewayConfig>
    findByProperty_PropertyId(Long propertyId);

    @EntityGraph(attributePaths = {"property", "gateway"})
    List<PaymentGatewayConfig> findAllByStatus(String status);
}