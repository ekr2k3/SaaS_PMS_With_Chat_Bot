package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.ListUsers;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/users")
@PreAuthorize("hasRole('OWNER')")
public class ListUsersController {

    private final ListUsersService listUsersService;

    public ListUsersController(
            ListUsersService listUsersService
    ) {
        this.listUsersService = listUsersService;
    }

    /**
     * GET /api/properties/{propertyId}/users
     *
     * Lấy danh sách nhân viên thuộc Property được chọn.
     * Chỉ SaaS Account Owner được truy cập.
     */
    @GetMapping
    public List<ListUsersResponse> listUsers(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return listUsersService.listUsers(
                principal.getUsername(),
                propertyId
        );
    }
}
