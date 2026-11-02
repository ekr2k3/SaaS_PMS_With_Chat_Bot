package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.ListRoles;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('OWNER')")
public class ListRolesController {

    private final ListRolesService listRolesService;

    public ListRolesController(ListRolesService listRolesService) {
        this.listRolesService = listRolesService;
    }

    // GET /api/roles
    @GetMapping
    public List<ListRolesResponse> listRoles() {
        return listRolesService.execute();
    }
}
