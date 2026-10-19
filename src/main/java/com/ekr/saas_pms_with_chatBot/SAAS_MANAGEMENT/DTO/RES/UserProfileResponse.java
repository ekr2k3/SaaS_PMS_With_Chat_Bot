package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private String userName;

    private String email;

    private String tenantName;

    private LocalDate startDate;

    private LocalDate endDate;

    private Subscription.SubscriptionStatus subscriptionStatus;

    private boolean showResubmitButton;
}