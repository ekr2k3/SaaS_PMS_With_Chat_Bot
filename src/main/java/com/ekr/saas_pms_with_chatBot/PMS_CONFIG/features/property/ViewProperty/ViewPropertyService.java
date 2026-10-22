package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.property.ViewProperty;


import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
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
public class ViewPropertyService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;

    public ViewPropertyService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
    }

    // Nghiep vu: Lay danh sach Property cua Owner.
    @Transactional(readOnly = true)
    public List<ViewPropertyResponse> getMyProperties(
            String ownerEmail
    ) {
        Tenant tenant = getOwnerTenant(ownerEmail);

        return propertyRepository
                .findAllByTenant_TenantIdOrderByPropertyIdAsc(
                        tenant.getTenantId()
                )
                .stream()
                .map(ViewPropertyResponse::from)
                .toList();
    }

    // Nghiep vu: Lay chi tiet Property cua Owner.
    @Transactional(readOnly = true)
    public ViewPropertyResponse getMyProperty(
            String ownerEmail,
            Long propertyId
    ) {
        Tenant tenant = getOwnerTenant(ownerEmail);

        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        return ViewPropertyResponse.from(property);
    }

    // Xac dinh Tenant tu Owner dang dang nhap.
    private Tenant getOwnerTenant(String ownerEmail) {
        Account account = accountRepository.findByEmail(ownerEmail);

        if (account == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Khong tim thay Owner"
            );
        }

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tai khoan Owner khong hoat dong"
            );
        }

        return tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Owner chua co Tenant"
                ));
    }
}
