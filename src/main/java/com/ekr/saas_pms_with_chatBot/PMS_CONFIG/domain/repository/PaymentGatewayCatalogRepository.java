
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PaymentGatewayCatalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentGatewayCatalogRepository
        extends JpaRepository<PaymentGatewayCatalog, Long> {

    Optional<PaymentGatewayCatalog> findByName(String name);

    boolean existsByName(String name);
}