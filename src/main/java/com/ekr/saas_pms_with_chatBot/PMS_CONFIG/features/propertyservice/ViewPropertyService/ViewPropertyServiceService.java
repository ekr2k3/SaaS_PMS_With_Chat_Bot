
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.propertyservice.ViewPropertyService;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PropertyService;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyServiceRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ViewPropertyServiceService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyServiceRepository propertyServiceRepository;

    public ViewPropertyServiceService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            PropertyServiceRepository propertyServiceRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.propertyServiceRepository = propertyServiceRepository;
    }

    @Transactional(readOnly = true)
    public List<ViewPropertyServiceResponse> getServices(
            String ownerEmail,
            Long propertyId
    ) {
        verifyPropertyAccess(ownerEmail, propertyId);

        return propertyServiceRepository
                .findAllByProperty_PropertyIdOrderByPropertyServiceIdAsc(
                        propertyId
                )
                .stream()
                .map(ViewPropertyServiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ViewPropertyServiceResponse getService(
            String ownerEmail,
            Long propertyId,
            Long propertyServiceId
    ) {
        verifyPropertyAccess(ownerEmail, propertyId);

        PropertyService propertyService =
                propertyServiceRepository
                        .findByPropertyServiceIdAndProperty_PropertyId(
                                propertyServiceId,
                                propertyId
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy cấu hình Service"
                        ));

        return ViewPropertyServiceResponse.from(propertyService);
    }

    private void verifyPropertyAccess(
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

        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Property"
                ));
    }
}