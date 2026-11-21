package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.configuration.CreatePaymentGatewayConfig;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PaymentGatewayCatalog;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PaymentGatewayConfig;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PaymentGatewayCatalogRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PaymentGatewayConfigRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;

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
public class CreatePaymentGatewayConfigService {

    private final AccountRepository accountRepository;

    private final TenantRepository tenantRepository;

    private final PropertyRepository propertyRepository;

    private final PaymentGatewayCatalogRepository catalogRepository;

    private final PaymentGatewayConfigRepository configRepository;

    private final StripeCredentialEncryptionService encryptionService;

    public CreatePaymentGatewayConfigService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            PaymentGatewayCatalogRepository catalogRepository,
            PaymentGatewayConfigRepository configRepository,
            StripeCredentialEncryptionService encryptionService
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.catalogRepository = catalogRepository;
        this.configRepository = configRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public CreatePaymentGatewayConfigResponse execute(
            String ownerEmail,
            Long propertyId,
            CreatePaymentGatewayConfigRequest request
    ) {

        // 1. Xác định Owner.
        Account account = accountRepository.findByEmail(ownerEmail);

        if (account == null ||
                account.getStatus() != Account.AccountStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Owner không hợp lệ hoặc đã bị vô hiệu hóa"
            );
        }

        // 2. Xác định Tenant.
        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Owner chưa được liên kết với Tenant"
                ));

        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tenant hiện không hoạt động"
            );
        }

        // 3. Kiểm tra Property thuộc Tenant của Owner.
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Property"
                ));

        // 4. Kiểm tra Property đã có cấu hình chưa.
        if (configRepository.existsByProperty_PropertyId(propertyId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Property đã có Payment Gateway Configuration"
            );
        }

        // 5. Lấy Stripe từ Catalog.
        PaymentGatewayCatalog gateway = catalogRepository
                .findByName("STRIPE")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Stripe chưa được khai báo trong Gateway Catalog"
                ));

        // 6. Kiểm tra Stripe có đang được hỗ trợ không.
        if (!"ACTIVE".equals(gateway.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Stripe hiện không được hỗ trợ"
            );
        }

        // 7. Mã hóa Credential.
        String encryptedSecretKey =
                encryptionService.encrypt(request.getSecretKey());

        String encryptedWebhookSecret =
                encryptionService.encrypt(request.getWebhookSecret());

        // 8. Tạo Configuration.
        PaymentGatewayConfig config = PaymentGatewayConfig.builder()
                .property(property)
                .gateway(gateway)
                .secretKey(encryptedSecretKey)
                .webhookSecret(encryptedWebhookSecret)
                .status("ACTIVE")
                .build();

        PaymentGatewayConfig saved =
                configRepository.saveAndFlush(config);

        // 9. Trả kết quả.
        return CreatePaymentGatewayConfigResponse.builder()
                .gatewayConfigId(saved.getGatewayConfigId())
                .propertyId(property.getPropertyId())
                .propertyName(property.getName())
                .gatewayName(gateway.getName())
                .status(saved.getStatus())
                .message("Cấu hình Stripe thành công")
                .build();
    }
}