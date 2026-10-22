package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.property.CreateProperty;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/properties")
public class CreatePropertyController {

    private final CreatePropertyService createPropertyService;

    public CreatePropertyController(
            CreatePropertyService createPropertyService
    ) {
        this.createPropertyService = createPropertyService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CreatePropertyResponse> createProperty(
            @Valid @ModelAttribute CreatePropertyRequest request,
            @RequestParam(
                    name = "image",
                    required = false
            ) MultipartFile image,
            @AuthenticationPrincipal UserDetails principal
    ) {
        CreatePropertyResponse response =
                createPropertyService.execute(
                        principal.getUsername(),
                        request,
                        image
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
