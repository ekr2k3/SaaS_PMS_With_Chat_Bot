package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.ListRoles;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Role;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListRolesService {

    private final RoleRepository roleRepository;

    public ListRolesService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<ListRolesResponse> execute() {
        return roleRepository
                .findAllByStatusOrderByRoleIdAsc(
                        Role.RoleStatus.ACTIVE
                )
                .stream()
                .map(role -> new ListRolesResponse(
                        role.getRoleId(),
                        role.getName(),
                        role.getDescription()
                ))
                .toList();
    }
}
