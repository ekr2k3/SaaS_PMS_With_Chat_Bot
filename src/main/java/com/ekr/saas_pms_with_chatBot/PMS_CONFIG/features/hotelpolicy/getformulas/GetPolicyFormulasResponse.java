package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.getformulas;


import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormula;

public record GetPolicyFormulasResponse(
        Long policyFormulaId,
        String policyType,
        String name,
        String status
) {

    public static GetPolicyFormulasResponse from(
            PolicyFormula policyFormula
    ) {
        return new GetPolicyFormulasResponse(
                policyFormula.getPolicyFormulaId(),
                policyFormula.getPolicyType(),
                policyFormula.getName(),
                policyFormula.getStatus()
        );
    }
}