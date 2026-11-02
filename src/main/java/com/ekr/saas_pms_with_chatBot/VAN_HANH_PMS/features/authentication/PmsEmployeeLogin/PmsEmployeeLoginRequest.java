package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.authentication.PmsEmployeeLogin;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PmsEmployeeLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
