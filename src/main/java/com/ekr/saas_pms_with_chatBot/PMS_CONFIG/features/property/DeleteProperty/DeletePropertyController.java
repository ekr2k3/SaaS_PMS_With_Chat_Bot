package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.property.DeleteProperty;


import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties")
@PreAuthorize("hasRole('OWNER')")
public class DeletePropertyController {

    private final DeletePropertyService deletePropertyService;

    public DeletePropertyController(
            DeletePropertyService deletePropertyService
    ) {
        this.deletePropertyService = deletePropertyService;
    }

    // API: DELETE /api/properties/{propertyId}
    @DeleteMapping("/{propertyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProperty(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        deletePropertyService.deleteProperty(
                principal.getUsername(),
                propertyId
        );
    }
}
