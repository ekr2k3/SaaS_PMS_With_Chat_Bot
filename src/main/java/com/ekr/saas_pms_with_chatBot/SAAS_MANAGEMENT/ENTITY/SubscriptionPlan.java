package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "subscription_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_unit", nullable = false, length = 10)
    private DurationUnit durationUnit;

    @Column(name = "max_users", nullable = false)
    private Integer maxUsers;

    @Column(name = "max_properties", nullable = false)
    private Integer maxProperties;

    @Column(name = "max_storage", nullable = false)
    private Long maxStorage;

    @Column(name = "max_rooms", nullable = false)
    private Integer maxRooms;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PlanStatus status;


    public enum DurationUnit {
        DAY,
        MONTH,
        YEAR
    }

    public enum PlanStatus {
        ACTIVE,
        INACTIVE
    }


    // Dam bao quan he 2 chieu

    @OneToMany(mappedBy = "plan")
    private List<Subscription> subscriptions;

}
