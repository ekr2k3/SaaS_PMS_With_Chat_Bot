package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.CreateUser;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.User;

public record CreateUserResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        String status,
        RoleResponse role
) {

    public static CreateUserResponse from(User user) {
        return new CreateUserResponse(
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
