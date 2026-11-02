
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.fnbmenu.UpdateFnbMenu;

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

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class UpdateFnbMenuService {

    private static final String ACTIVE = "ACTIVE";
    private static final String FNB_SERVICE_TYPE = "FNB";

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final PropertyServiceRepository propertyServiceRepository;
    private final FnbMenuRepository fnbMenuRepository;

    public UpdateFnbMenuService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            ServiceCatalogRepository serviceCatalogRepository,
            PropertyServiceRepository propertyServiceRepository,
            FnbMenuRepository fnbMenuRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.propertyServiceRepository = propertyServiceRepository;
        this.fnbMenuRepository = fnbMenuRepository;
    }

    @Transactional
    public FnbMenu update(
            String ownerEmail,
            Long propertyId,
            Long menuId,
            UpdateFnbMenuRequest request
    ) {
        Property property = verifyPropertyAccess(ownerEmail, propertyId);

        FnbMenu menu = fnbMenuRepository
                .findByFnbMenuIdAndProperty_PropertyId(
                        menuId,
                        property.getPropertyId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy món F&B"
                ));

        if (request.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cần cung cấp ít nhất một trường để cập nhật"
            );
        }

        validateFnbServiceIsActive(propertyId);

        if (request.name() != null) {
            String name = request.name().trim();

            if (name.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tên món không được để trống"
                );
            }

            menu.setName(name);
        }

        if (request.maxQuantity() != null) {
            menu.setMaxQuantity(request.maxQuantity());
        }

        if (request.actualQuantity() != null) {
            menu.setActualQuantity(request.actualQuantity());
        }

        if (request.purchasePrice() != null) {
            menu.setPurchasePrice(request.purchasePrice());
        }

        if (request.sellingPricePercent() != null) {
            menu.setSellingPricePercent(request.sellingPricePercent());
        }

        if (menu.getActualQuantity() > menu.getMaxQuantity()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "actualQuantity không được lớn hơn maxQuantity"
            );
        }

        menu.setSellingPrice(calculateSellingPrice(
                menu.getPurchasePrice(),
                menu.getSellingPricePercent()
        ));

        return fnbMenuRepository.save(menu);
    }

    private Property verifyPropertyAccess(
            String ownerEmail,
            Long propertyId
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

        return propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Property"
                ));
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

    private BigDecimal calculateSellingPrice(
            BigDecimal purchasePrice,
            BigDecimal sellingPricePercent
    ) {
        BigDecimal multiplier = BigDecimal.ONE.add(
                sellingPricePercent.divide(
                        new BigDecimal("100"),
                        6,
                        RoundingMode.HALF_UP
                )
        );

        return purchasePrice.multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
