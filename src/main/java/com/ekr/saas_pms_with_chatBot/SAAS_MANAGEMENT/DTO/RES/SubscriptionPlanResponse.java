package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanResponse {

    private Long planId;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer duration;

    private SubscriptionPlan.DurationUnit durationUnit;

    private Integer maxUsers;

    private Integer maxProperties;

    private Long maxStorage;

    private Integer maxRooms;

    private SubscriptionPlan.PlanStatus status;
}
