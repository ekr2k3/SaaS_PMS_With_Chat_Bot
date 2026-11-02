package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.ViewUser;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Permission;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.User;

import java.util.List;

public record ViewUserResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        String status,
        Long roleId,
        String roleName,
        List<PermissionResponse> permissions
) {

    public static ViewUserResponse from(
            User user,
            List<Permission> permissions
    ) {
        return new ViewUserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus().name(),
                user.getRole().getRoleId(),
                user.getRole().getName(),
                permissions.stream()
                        .map(PermissionResponse::from)
                        .toList()
        );
    }

    public record PermissionResponse(
            String code,
            String name
    ) {

        public static PermissionResponse from(Permission permission) {
            return new PermissionResponse(
                    permission.getCode(),
                    permission.getName()
            );
        }
    }
}
