
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.propertyservice.UpdatePropertyServiceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdatePropertyServiceStatusRequest(

        @NotBlank(message = "Status là bắt buộc")
        @Pattern(
                regexp = "ACTIVE|INACTIVE",
                message = "Status chỉ được là ACTIVE hoặc INACTIVE"
        )
        String status

) {
}