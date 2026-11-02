package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.UpdateUser;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.User;

public record UpdateUserResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        String status,
        RoleResponse role
) {

    public static UpdateUserResponse from(User user) {
        return new UpdateUserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus().name(),
                new RoleResponse(
                        user.getRole().getRoleId(),
                        user.getRole().getName(),
                        user.getRole().getDescription()
                )
        );
    }

    public record RoleResponse(
            Long roleId,
            String name,
            String description
    ) {
    }
}
