package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES;


import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor //(chỉ cho field private @NotNull)
public class LoginResponseDTO {
    private String message;
    private String token;
}
