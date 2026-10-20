package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES.UserProfileResponse;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {

    private final AccountRepository accountRepository;

    private final TenantRepository tenantRepository;

    private final SubscriptionRepository subscriptionRepository;

    public UserProfileService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            SubscriptionRepository subscriptionRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String email) {

        // 1. Tìm Account từ email lấy trong JWT
        Account account = accountRepository.findByEmail(email);

        if (account == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy tài khoản đang đăng nhập"
            );
        }

        // 2. Tìm Tenant liên kết với Account
        Tenant tenant = tenantRepository
                .findByAccount(account)
                .orElse(null);

        // 3. Tìm Subscription của Tenant
        Subscription subscription = tenant == null
                ? null
                : subscriptionRepository
                .findByTenant(tenant)
                .orElse(null);

        // 4. Kiểm tra trạng thái ACTIVE
        boolean isActive = subscription != null
                && subscription.getStatus()
                == Subscription.SubscriptionStatus.ACTIVE;

        // 5. Xác định có hiển thị nút gửi lại yêu cầu không
        boolean showResubmitButton = subscription != null
                && subscription.getStatus()
                == Subscription.SubscriptionStatus.REJECTED;

        boolean showRetryPaymentButton = subscription != null
                && subscription.getStatus()
                == Subscription.SubscriptionStatus.PENDING_PAYMENT;

        // 6. Tạo DTO Response
        return new UserProfileResponse(
                account.getName(),
                account.getEmail(),
                tenant == null ? null : tenant.getName(),

                // Chỉ hiển thị thời hạn khi ACTIVE
                isActive ? subscription.getStartDate() : null,
                isActive ? subscription.getEndDate() : null,

                // Trả trạng thái Subscription hiện tại
                subscription == null
                        ? null
                        : subscription.getStatus(),

                showResubmitButton,
                showRetryPaymentButton
        );
    }
}