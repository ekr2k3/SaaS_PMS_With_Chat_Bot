package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;


import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormula;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyFormulaRepository
        extends JpaRepository<PolicyFormula, Long> {

    boolean existsByPolicyTypeAndName(
            String policyType,
            String name
    );
}
