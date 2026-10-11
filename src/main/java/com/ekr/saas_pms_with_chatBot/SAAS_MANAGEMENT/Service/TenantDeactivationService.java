package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantDeactivationService {

    private final TenantRepository tenantRepository;

    /**
     * 1.4F18 - Vô hiệu hóa Tenant khi Subscription hết hạn.
     */
    @Transactional
    public boolean deactivateTenant(Subscription subscription) {

        if (subscription == null || subscription.getTenant() == null) {
            throw new IllegalArgumentException(
                    "Subscription hoặc Tenant không hợp lệ"
            );
        }

        Tenant tenant = subscription.getTenant();

        // Không tự ý thay đổi trạng thái SUSPENDED
        // do quản trị hoặc chính sách khác áp dụng.
        if (tenant.getStatus() == Tenant.TenantStatus.SUSPENDED) {
            return false;
        }

        if (tenant.getStatus() == Tenant.TenantStatus.INACTIVE) {
            return false;
        }

        tenant.setStatus(Tenant.TenantStatus.INACTIVE);

        tenantRepository.save(tenant);

        return true;
    }
}