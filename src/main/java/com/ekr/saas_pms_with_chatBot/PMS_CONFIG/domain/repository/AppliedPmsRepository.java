package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.AppliedPms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppliedPmsRepository
        extends JpaRepository<
        AppliedPms,
        AppliedPms.AppliedPmsId
        > {

    List<AppliedPms> findAllByPropertyIdOrderByPolicyFormulaIdAsc(
            Long propertyId
    );
}
