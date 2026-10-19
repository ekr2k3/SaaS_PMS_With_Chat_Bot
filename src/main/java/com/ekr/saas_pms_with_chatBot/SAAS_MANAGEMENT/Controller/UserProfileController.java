package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES.UserProfileResponse;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(
            UserProfileService userProfileService
    ) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<UserProfileResponse> getProfile(
            Authentication authentication
    ) {

        UserProfileResponse response =
                userProfileService.getCurrentUserProfile(
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }
}