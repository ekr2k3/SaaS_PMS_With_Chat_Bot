
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.configure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record ConfigureHotelPoliciesRequest(
        @NotNull
        List<@Valid PolicyConfiguration> policies
) {

    public record PolicyConfiguration(
            @NotNull
            Long policyFormulaId,

            Map<String, String> parameters
    ) {
    }
}
