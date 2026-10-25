
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.configure;

import java.util.List;

public record ConfigureHotelPoliciesResponse(
        Long propertyId,
        String message,
        List<ConfiguredPolicy> policies
) {

    public record ConfiguredPolicy(
            Long policyFormulaId,
            String policyType,
            String name,
            String status
    ) {
    }
}