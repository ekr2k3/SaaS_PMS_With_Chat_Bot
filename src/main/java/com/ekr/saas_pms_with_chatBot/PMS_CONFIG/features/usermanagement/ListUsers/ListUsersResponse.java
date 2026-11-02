package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.ListUsers;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.User;

public record ListUsersResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        String status,
        Long roleId,
        String roleName
) {

    public static ListUsersResponse from(User user) {
        return new ListUsersResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus().name(),
                user.getRole().getRoleId(),
                user.getRole().getName()
        );
    }
}
