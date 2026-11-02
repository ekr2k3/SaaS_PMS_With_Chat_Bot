
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.fnbmenu.CreateFnbMenu;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.fnbmenu.FnbMenuResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/fnb-menu")
@PreAuthorize("hasRole('OWNER')")
public class CreateFnbMenuController {

    private final CreateFnbMenuService createFnbMenuService;

    public CreateFnbMenuController(
            CreateFnbMenuService createFnbMenuService
    ) {
        this.createFnbMenuService = createFnbMenuService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FnbMenuResponse create(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @Valid @RequestBody CreateFnbMenuRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chưa đăng nhập"
            );
        }

        return FnbMenuResponse.from(
                createFnbMenuService.create(
                        principal.getUsername(),
                        propertyId,
                        request
                )
        );
    }
}