package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.UpdateUser;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @NotBlank(message = "Ho ten khong duoc de trong")
        @Size(max = 150, message = "Ho ten toi da 150 ky tu")
        String fullName,

        @Size(max = 20, message = "So dien thoai toi da 20 ky tu")
        String phone,

        @NotNull(message = "Role khong duoc de trong")
        Long roleId
) {
}
