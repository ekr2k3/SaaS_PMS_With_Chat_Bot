package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.authentication.PmsEmployeeLogin;

import java.util.List;

public record PmsEmployeeLoginResponse(
        String message,
        String accessToken,
        UserInfo user,
        List<String> permissions
) {
    public record UserInfo(
            Long userId,
            String fullName,
            String email,
            Long propertyId,
            String propertyName,
            Long tenantId,
            String role
    ) {
    }
}
