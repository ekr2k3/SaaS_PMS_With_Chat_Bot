
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.propertyservice.RegisterPropertyService;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegisterPropertyServiceRequest(

        @NotNull(message = "Service ID là bắt buộc")
        @Positive(message = "Service ID phải lớn hơn 0")
        Long serviceId

) {
}