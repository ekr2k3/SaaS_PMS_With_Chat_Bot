package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;





import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormulaParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyFormulaParameterRepository
        extends JpaRepository<
        PolicyFormulaParameter,
        PolicyFormulaParameter.PolicyFormulaParameterId
        > {

    List<PolicyFormulaParameter>
    findAllByPropertyIdOrderByPolicyFormulaIdAscParamNameAsc(
            Long propertyId
    );

    void deleteAllByPropertyIdAndPolicyFormulaId(
            Long propertyId,
            Long policyFormulaId
    );
}
