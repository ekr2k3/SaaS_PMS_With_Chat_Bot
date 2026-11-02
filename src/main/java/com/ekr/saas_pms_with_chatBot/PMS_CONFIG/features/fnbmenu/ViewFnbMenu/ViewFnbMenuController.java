
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.fnbmenu.ViewFnbMenu;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.fnbmenu.FnbMenuResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/fnb-menu")
@PreAuthorize("hasRole('OWNER')")
public class ViewFnbMenuController {

    private final ViewFnbMenuService viewFnbMenuService;

    public ViewFnbMenuController(
            ViewFnbMenuService viewFnbMenuService
    ) {
        this.viewFnbMenuService = viewFnbMenuService;
    }

    @GetMapping
    public List<FnbMenuResponse> getMenus(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chưa đăng nhập"
            );
        }

        return viewFnbMenuService.getMenus(
                principal.getUsername(),
                propertyId
        );
    }

    @GetMapping("/{menuId}")
    public FnbMenuResponse getMenu(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @PathVariable Long menuId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chưa đăng nhập"
            );
        }

        return viewFnbMenuService.getMenu(
                principal.getUsername(),
                propertyId,
                menuId
        );
    }
}