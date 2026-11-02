
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.propertyservice.RegisterPropertyService;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PropertyService;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.ServiceCatalog;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyServiceRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.ServiceCatalogRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class RegisterPropertyServiceService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final PropertyServiceRepository propertyServiceRepository;

    public RegisterPropertyServiceService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            ServiceCatalogRepository serviceCatalogRepository,
            PropertyServiceRepository propertyServiceRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.propertyServiceRepository = propertyServiceRepository;
    }

    @Transactional
    public RegisterPropertyServiceResponse register(
            String ownerEmail,
            Long propertyId,
            RegisterPropertyServiceRequest request
    ) {
        Account account = accountRepository.findByEmail(ownerEmail);

        if (account == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Không tìm thấy Owner"
            );
        }

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản Owner không hoạt động"
            );
        }

        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Owner chưa có Tenant"
                ));

        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Property"
                ));

        ServiceCatalog service = serviceCatalogRepository
                .findById(request.serviceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Service trong Catalog"
                ));

        if (!ACTIVE.equals(service.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Service đã ngừng được SaaS cung cấp"
            );
        }

        Optional<PropertyService> existingOptional =
                propertyServiceRepository
                        .findByProperty_PropertyIdAndService_ServiceId(
                                property.getPropertyId(),
                                service.getServiceId()
                        );

        if (existingOptional.isPresent()) {
            PropertyService existing = existingOptional.get();

            if (ACTIVE.equals(existing.getStatus())) {
                return RegisterPropertyServiceResponse.from(
                        existing,
                        "Service đã được đăng ký trước đó"
                );
            }

            if (!INACTIVE.equals(existing.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Trạng thái cấu hình Service không hợp lệ"
                );
            }

            existing.setStatus(ACTIVE);

            PropertyService reactivated =
                    propertyServiceRepository.save(existing);

            return RegisterPropertyServiceResponse.from(
                    reactivated,
                    "Đã kích hoạt lại Service"
            );
        }

        PropertyService propertyService = PropertyService.builder()
                .property(property)
                .service(service)
                .status(ACTIVE)
                .build();

        PropertyService saved =
                propertyServiceRepository.save(propertyService);

        return RegisterPropertyServiceResponse.from(
                saved,
                "Đăng ký Service thành công"
        );
    }
}