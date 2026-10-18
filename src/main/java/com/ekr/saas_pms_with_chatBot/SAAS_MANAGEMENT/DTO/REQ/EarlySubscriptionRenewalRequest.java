package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EarlySubscriptionRenewalRequest {

    @NotNull(message = "Plan ID không được để trống")
    @Positive(message = "Plan ID phải lớn hơn 0")
    private Long planId;
}
