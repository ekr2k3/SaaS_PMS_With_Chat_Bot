package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor //(chỉ cho field private @NotNull)
public class LoginRequestDTO {
    private String email;
    private String password;
}
