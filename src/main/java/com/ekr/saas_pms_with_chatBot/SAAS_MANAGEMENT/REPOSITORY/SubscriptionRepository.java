package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByTenant(Tenant tenant);

    Optional<Subscription> findByTenant(Tenant tenant);
}