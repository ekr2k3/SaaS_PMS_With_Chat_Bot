package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

public record SubscriptionExpirationResult(
        boolean expired,
        Long subscriptionId,
        String ownerEmail,
        String ownerName,
        String tenantName
) {
}
