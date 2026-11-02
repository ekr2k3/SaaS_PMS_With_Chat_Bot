package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.CreateUser;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "Ho ten khong duoc de trong")
        @Size(max = 150, message = "Ho ten toi da 150 ky tu")
        String fullName,

        @NotBlank(message = "Email khong duoc de trong")
        @Email(message = "Email khong dung dinh dang")
        @Size(max = 255, message = "Email toi da 255 ky tu")
        String email,

        @Size(max = 20, message = "So dien thoai toi da 20 ky tu")
        String phone,

        @NotBlank(message = "Mat khau khong duoc de trong")
        @Size(min = 8, max = 72,
                message = "Mat khau phai tu 8 den 72 ky tu")
        String password,

        @NotNull(message = "Role khong duoc de trong")
        Long roleId
) {
}
