
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.fnbmenu.ViewFnbMenu;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.FnbMenu;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.FnbMenuRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.fnbmenu.FnbMenuResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ViewFnbMenuService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final FnbMenuRepository fnbMenuRepository;

    public ViewFnbMenuService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            FnbMenuRepository fnbMenuRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.fnbMenuRepository = fnbMenuRepository;
    }

    @Transactional(readOnly = true)
    public List<FnbMenuResponse> getMenus(
            String ownerEmail,
            Long propertyId
    ) {
        verifyPropertyAccess(ownerEmail, propertyId);

        return fnbMenuRepository
                .findAllByProperty_PropertyIdOrderByFnbMenuIdAsc(propertyId)
                .stream()
                .map(FnbMenuResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FnbMenuResponse getMenu(
            String ownerEmail,
            Long propertyId,
            Long menuId
    ) {
        verifyPropertyAccess(ownerEmail, propertyId);

        FnbMenu menu = fnbMenuRepository
                .findByFnbMenuIdAndProperty_PropertyId(
                        menuId,
                        propertyId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy món F&B"
                ));

        return FnbMenuResponse.from(menu);
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

        propertyRepository.findByPropertyIdAndTenant_TenantId(
                propertyId,
                tenant.getTenantId()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy Property"
        ));
    }
}