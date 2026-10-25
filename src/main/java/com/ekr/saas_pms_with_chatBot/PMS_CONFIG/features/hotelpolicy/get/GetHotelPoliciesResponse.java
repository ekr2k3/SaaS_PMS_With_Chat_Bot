package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.get;



import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormula;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormulaParameter;


import java.util.List;

public record GetHotelPoliciesResponse(
        Long propertyId,
        List<PolicyResponse> policies
) {

    public record PolicyResponse(
            Long policyFormulaId,
            String policyType,
            String name,
            String status,
            List<ParameterResponse> parameters
    ) {

        public static PolicyResponse from(
                PolicyFormula formula,
                List<PolicyFormulaParameter> parameters
        ) {
            return new PolicyResponse(
                    formula.getPolicyFormulaId(),
                    formula.getPolicyType(),
                    formula.getName(),
                    formula.getStatus(),
                    parameters.stream()
                            .map(ParameterResponse::from)
                            .toList()
            );
        }
    }

    public record ParameterResponse(
            String paramName,
            String paramValue
    ) {

        public static ParameterResponse from(
                PolicyFormulaParameter parameter
        ) {
            return new ParameterResponse(
                    parameter.getParamName(),
                    parameter.getParamValue()
            );
        }
    }
}
