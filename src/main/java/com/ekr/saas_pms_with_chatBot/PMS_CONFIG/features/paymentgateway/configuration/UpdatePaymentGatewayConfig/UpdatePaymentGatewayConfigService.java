package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.configuration.UpdatePaymentGatewayConfig;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PaymentGatewayConfig;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;


import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PaymentGatewayConfigRepository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.security.StripeCredentialEncryptionService;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UpdatePaymentGatewayConfigService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final PaymentGatewayConfigRepository configRepository;
    private final StripeCredentialEncryptionService encryptionService;

    public UpdatePaymentGatewayConfigService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            PaymentGatewayConfigRepository configRepository,
            StripeCredentialEncryptionService encryptionService
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.configRepository = configRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public UpdatePaymentGatewayConfigResponse execute(
            String ownerEmail,
            Long propertyId,
            UpdatePaymentGatewayConfigRequest request
    ) {

        //
        if (request.getSecretKey() == null
                && request.getWebhookSecret() == null
                && request.getStatus() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không có thông tin cần cập nhật"
            );
        }

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

        // 3. Kiểm tra Property thuộc Owner.
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Property"
                ));

        // 4. Tìm cấu hình hiện tại.
        PaymentGatewayConfig config = configRepository
                .findByProperty_PropertyId(propertyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Property chưa có Payment Gateway Configuration"
                ));

        // 5. Cập nhật Secret Key nếu được gửi lên.
        if (request.getSecretKey() != null) {

            config.setSecretKey(
                    encryptionService.encrypt(
                            request.getSecretKey()
                    )
            );
        }

        // 6. Cập nhật Webhook Secret nếu được gửi lên.
        if (request.getWebhookSecret() != null) {

            config.setWebhookSecret(
                    encryptionService.encrypt(
                            request.getWebhookSecret()
                    )
            );
        }

        // 7. Cập nhật trạng thái nếu được gửi lên.
        if (request.getStatus() != null) {

            config.setStatus(request.getStatus());
        }

        // 8. Lưu thay đổi.
        PaymentGatewayConfig saved =
                configRepository.saveAndFlush(config);

        // 9. Trả kết quả.
        return UpdatePaymentGatewayConfigResponse.builder()
                .gatewayConfigId(saved.getGatewayConfigId())
                .propertyId(property.getPropertyId())
                .gatewayName(saved.getGateway().getName())
                .status(saved.getStatus())
                .message("Cập nhật Payment Gateway thành công")
                .build();
    }
}