package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LateSubscriptionRenewalRequest {

    @NotNull
    private Long planId;
}
