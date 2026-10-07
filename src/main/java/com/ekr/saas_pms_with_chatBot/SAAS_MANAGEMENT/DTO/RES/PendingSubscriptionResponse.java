package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PendingSubscriptionResponse {

    private Long subscriptionId;

    private String email;

    private String plan;

    private String tenantName;

    private String tenantDescription;
}
