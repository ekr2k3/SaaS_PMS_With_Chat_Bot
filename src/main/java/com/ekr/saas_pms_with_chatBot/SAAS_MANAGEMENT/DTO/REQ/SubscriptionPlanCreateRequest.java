package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ;


import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanCreateRequest {

    @NotBlank(message = "Tên Plan không được để trống")
    @Size(max = 100, message = "Tên Plan không được vượt quá 100 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Giá Plan không được để trống")
    @DecimalMin(value = "0.0", inclusive = true,
            message = "Giá Plan phải >= 0")
    @Digits(integer = 13, fraction = 2,
            message = "Giá Plan không hợp lệ")
    private BigDecimal price;

    @NotNull(message = "Thời hạn không được để trống")
    @Min(value = 1,
            message = "Thời hạn phải > 0")
    private Integer duration;

    @NotNull(message = "Đơn vị thời hạn không được để trống")
    private SubscriptionPlan.DurationUnit durationUnit;

    @NotNull(message = "Giới hạn User không được để trống")
    @Min(value = 0,
            message = "maxUsers phải >= 0")
    private Integer maxUsers;

    @NotNull(message = "Giới hạn Property không được để trống")
    @Min(value = 0,
            message = "maxProperties phải >= 0")
    private Integer maxProperties;

    @NotNull(message = "Giới hạn Storage không được để trống")
    @Min(value = 0,
            message = "maxStorage phải >= 0")
    private Long maxStorage;

    @NotNull(message = "Giới hạn Room không được để trống")
    @Min(value = 0,
            message = "maxRooms phải >= 0")
    private Integer maxRooms;
}
