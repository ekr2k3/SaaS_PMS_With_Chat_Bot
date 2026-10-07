package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ;


import jakarta.validation.constraints.*;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequestDTO {

    @NotNull
    private Long planId;

    @NotBlank
    @Size(max = 150)
    private String name;

    private String description;
}
