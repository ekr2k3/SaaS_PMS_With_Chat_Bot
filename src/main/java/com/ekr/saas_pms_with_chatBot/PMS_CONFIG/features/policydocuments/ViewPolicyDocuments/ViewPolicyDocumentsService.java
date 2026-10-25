package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.policydocuments.ViewPolicyDocuments;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.FileMetadataRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ViewPolicyDocumentsService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final FileMetadataRepository fileMetadataRepository;

    public ViewPolicyDocumentsService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            FileMetadataRepository fileMetadataRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    @Transactional(readOnly = true)
    public ViewPolicyDocumentsResponse execute(
            String ownerEmail,
            Long propertyId
    ) {
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

        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Owner chua co Tenant"
                ));

        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        List<ViewPolicyDocumentsResponse.DocumentItem> documents =
                fileMetadataRepository
                        .findByProperty_PropertyIdAndAccount_AccountId(
                                property.getPropertyId(),
                                account.getAccountId()
                        )
                        .stream()
                        .map(ViewPolicyDocumentsResponse.DocumentItem::from)
                        .toList();

        return new ViewPolicyDocumentsResponse(
                property.getPropertyId(),
                documents
        );
    }
}
