
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.fnbmenu.UpdateFnbMenuStatus;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.FnbMenu;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PropertyService;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.ServiceCatalog;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.FnbMenuRepository;
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

@Service
public class UpdateFnbMenuStatusService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";
    private static final String FNB_SERVICE_TYPE = "FNB";

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final FnbMenuRepository fnbMenuRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final PropertyServiceRepository propertyServiceRepository;

    public UpdateFnbMenuStatusService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            FnbMenuRepository fnbMenuRepository,
            ServiceCatalogRepository serviceCatalogRepository,
            PropertyServiceRepository propertyServiceRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.fnbMenuRepository = fnbMenuRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.propertyServiceRepository = propertyServiceRepository;
    }

    @Transactional
    public FnbMenu updateStatus(
            String ownerEmail,
            Long propertyId,
            Long menuId,
            UpdateFnbMenuStatusRequest request
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

        FnbMenu menu = fnbMenuRepository
                .findByFnbMenuIdAndProperty_PropertyId(
                        menuId,
                        property.getPropertyId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy món F&B"
                ));

        String requestedStatus = request.status();

        if (ACTIVE.equals(requestedStatus)) {
            validateFnbServiceIsActive(propertyId);
        } else if (!INACTIVE.equals(requestedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status chỉ được là ACTIVE hoặc INACTIVE"
            );
        }

        // Idempotent: nếu trạng thái không đổi thì giữ nguyên.
        if (requestedStatus.equals(menu.getStatus())) {
            return menu;
        }

        menu.setStatus(requestedStatus);

        return fnbMenuRepository.save(menu);
    }

    private void validateFnbServiceIsActive(Long propertyId) {
        ServiceCatalog catalog = serviceCatalogRepository
                .findByServiceType(FNB_SERVICE_TYPE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Catalog F&B chưa được cấu hình"
                ));

        if (!ACTIVE.equals(catalog.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Dịch vụ F&B đã ngừng được SaaS cung cấp"
            );
        }

        PropertyService propertyService = propertyServiceRepository
                .findByProperty_PropertyIdAndService_ServiceId(
                        propertyId,
                        catalog.getServiceId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Property chưa đăng ký dịch vụ F&B"
                ));

        if (!ACTIVE.equals(propertyService.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Dịch vụ F&B của Property chưa được kích hoạt"
            );
        }
    }
}