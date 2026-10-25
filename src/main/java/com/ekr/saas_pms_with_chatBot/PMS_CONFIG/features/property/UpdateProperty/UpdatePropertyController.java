package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.property.UpdateProperty;



import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.property.ViewProperty.ViewPropertyResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties")
@PreAuthorize("hasRole('OWNER')")
public class UpdatePropertyController {

    private final UpdatePropertyService updatePropertyService;

    public UpdatePropertyController(
            UpdatePropertyService updatePropertyService
    ) {
        this.updatePropertyService = updatePropertyService;
    }

    // API: PUT /api/properties/{propertyId}
    // Cap nhat thong tin Property cua Owner dang dang nhap.
    @PutMapping(
            value = "/{propertyId}",
            consumes = "multipart/form-data"
    )
    public ViewPropertyResponse updateProperty(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @Valid @ModelAttribute UpdatePropertyRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return updatePropertyService.updateProperty(
                principal.getUsername(),
                propertyId,
                request
        );
    }
}
