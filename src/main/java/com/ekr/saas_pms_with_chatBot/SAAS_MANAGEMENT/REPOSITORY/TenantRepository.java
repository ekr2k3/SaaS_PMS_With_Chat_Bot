package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    boolean existsByAccount(Account account);

    Optional<Tenant> findByAccount(Account account);
}
