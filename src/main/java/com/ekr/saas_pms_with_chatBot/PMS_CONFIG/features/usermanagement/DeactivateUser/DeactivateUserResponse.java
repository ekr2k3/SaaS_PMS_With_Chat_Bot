package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.DeactivateUser;

public record DeactivateUserResponse(
        Long userId,
        String status,
        String message
) {
}
