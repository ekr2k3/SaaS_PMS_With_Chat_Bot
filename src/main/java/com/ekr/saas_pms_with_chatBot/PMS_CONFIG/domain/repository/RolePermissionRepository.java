
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Permission;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RolePermission;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RolePermissionId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository
        extends JpaRepository<RolePermission, RolePermissionId> {

    boolean existsByRole_RoleIdAndPermission_PermissionId(
            Long roleId,
            Long permissionId
    );

    void deleteAllByPermission_PermissionId(Long permissionId);

    void deleteAllByPermission_PermissionIdAndRole_RoleIdNot(
            Long permissionId,
            Long roleId
    );

    @EntityGraph(attributePaths = "permission")
    List<RolePermission> findAllByRole_RoleId(Long roleId);

    @EntityGraph(attributePaths = "permission")
    List<RolePermission> findAllByRole_RoleIdAndPermission_Status(
            Long roleId,
            Permission.PermissionStatus permissionStatus
    );

    @EntityGraph(attributePaths = "permission")
    List<RolePermission> findAllByRole_NameAndPermission_Status(
            String roleName,
            Permission.PermissionStatus permissionStatus
    );
}