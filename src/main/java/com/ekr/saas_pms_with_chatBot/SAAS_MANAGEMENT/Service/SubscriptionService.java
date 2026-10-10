package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ.SubscriptionRequestDTO;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionPlanRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class SubscriptionService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private MailService mailService;

    public ResponseEntity<?> sendRequestSubscription(SubscriptionRequestDTO req, Authentication auth){

        // =========================
        // 1. Lấy email từ JWT
        // =========================

        String email = auth.getName();


        // =========================
        // 2. Tìm Account
        // =========================

        Account account = accountRepository.findByEmail(email);
        if(account == null){
            ResponseEntity.status(409).body("Không tìm thấy tài khoản");
        }


        // =========================
        // 3. Kiểm tra Account
        //    đã có (Liên kết) Tenant chưa
        // =========================

        if (tenantRepository.existsByAccount(account) == true) {
            return ResponseEntity.badRequest().body(
                    "Account đã sở hữu Tenant. " +
                            "Vui lòng sử dụng Account mới."
            );
        }


        // =========================
        // 4. Kiểm tra Plan có tồn tại không
        // =========================

        SubscriptionPlan plan = subscriptionPlanRepository.findByPlanId(req.getPlanId());
        if(plan == null){
            return ResponseEntity.status(409).body("Plan không tồn tại");
        }


        // =========================
        // 5. Kiểm tra Plan ACTIVE
        // =========================

        if (plan.getStatus() != SubscriptionPlan.PlanStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(
                    "Subscription Plan hiện không hoạt động"
            );
        }


        // =========================
        // 6. Tạo Tenant
        // =========================

        Tenant tenant = Tenant.builder()
                .name(req.getName())
                .description(req.getDescription())
                .status(Tenant.TenantStatus.INACTIVE)
                .account(account)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);


        // =========================
        // 7. Tạo Subscription
        // =========================

        Subscription subscription = Subscription.builder()
                .status(Subscription.SubscriptionStatus.PENDING)
                .startDate(null)
                .endDate(null)
                .plan(plan)
                .tenant(savedTenant)
                .build();

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        // =========================
        // 9. Bổ sung gửi email
        // =========================

        mailService.sendSubscriptionRequestNotification(
                account.getEmail(),
                savedTenant.getName(),
                savedTenant.getTenantId(),
                savedSubscription.getSubscriptionId(),
                plan.getName(),
                savedTenant.getStatus().name(),
                savedSubscription.getStatus().name()
        );

        // =========================
        // 8. Response
        // =========================

        return ResponseEntity.ok(
                Map.of(
                        "tenantId",
                        savedTenant.getTenantId(),

                        "tenantName",
                        savedTenant.getName(),

                        "tenantStatus",
                        savedTenant.getStatus(),

                        "subscriptionId",
                        savedSubscription.getSubscriptionId(),

                        "subscriptionStatus",
                        savedSubscription.getStatus(),

                        "planId",
                        plan.getPlanId()
                )
        );
    }

}
