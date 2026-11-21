package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.configuration.ViewPaymentGatewayConfig;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PaymentGatewayConfig;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;


import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PaymentGatewayConfigRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;



import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ViewPaymentGatewayConfigService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final PaymentGatewayConfigRepository configRepository;

    public ViewPaymentGatewayConfigService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            PaymentGatewayConfigRepository configRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.configRepository = configRepository;
    }

    @Transactional(readOnly = true)
    public ViewPaymentGatewayConfigResponse execute(
            String ownerEmail,
            Long propertyId
    ) {

        // 1. Xác định Owner.
        Account account = accountRepository.findByEmail(ownerEmail);

        if (account == null ||
                account.getStatus() != Account.AccountStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Owner không hợp lệ"
            );
        }

        // 2. Xác định Tenant.
        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Owner chưa có Tenant"
                ));

        // 3. Kiểm tra quyền sở hữu Property.
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Property"
                ));

        // 4. Lấy cấu hình.
        PaymentGatewayConfig config = configRepository
                .findByProperty_PropertyId(propertyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Property chưa cấu hình Payment Gateway"
                ));

        // 5. Trả thông tin an toàn.
        return ViewPaymentGatewayConfigResponse.builder()
                .gatewayConfigId(config.getGatewayConfigId())
                .propertyId(property.getPropertyId())
                .propertyName(property.getName())
                .gatewayId(config.getGateway().getGatewayId())
                .gatewayName(config.getGateway().getName())
                .status(config.getStatus())
                .secretKeyConfigured(
                        config.getSecretKey() != null
                )
                .webhookSecretConfigured(
                        config.getWebhookSecret() != null
                )
                .build();
    }
}