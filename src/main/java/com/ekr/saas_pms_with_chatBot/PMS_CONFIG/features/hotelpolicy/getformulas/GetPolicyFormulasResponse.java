
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.getformulas;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormula;

import java.math.BigDecimal;
import java.util.List;

public record GetPolicyFormulasResponse(
        Long policyFormulaId,
        String policyType,
        String name,
        String status,
        List<ParameterDefinition> parameters
) {

    public record ParameterDefinition(
            String name,
            String type,
            boolean required,
            BigDecimal min,
            BigDecimal max,
            BigDecimal minExclusive
    ) {
    }

    public static GetPolicyFormulasResponse from(
            PolicyFormula policyFormula,
            List<ParameterDefinition> parameters
    ) {
        return new GetPolicyFormulasResponse(
                policyFormula.getPolicyFormulaId(),
                policyFormula.getPolicyType(),
                policyFormula.getName(),
                policyFormula.getStatus(),
                List.copyOf(parameters)
        );
    }
}