package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository  extends JpaRepository<SubscriptionPlan, Long> {
    SubscriptionPlan findByPlanId (Long id); // Phải giống tên field trong entity
}