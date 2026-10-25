package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.property.UpdateProperty;


import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.property.ViewProperty.ViewPropertyResponse;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.infrastructure.storage.PropertyImageStorageService;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UpdatePropertyService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyImageStorageService imageStorageService;

    public UpdatePropertyService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            PropertyImageStorageService imageStorageService
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional
    public ViewPropertyResponse updateProperty(
            String ownerEmail,
            Long propertyId,
            UpdatePropertyRequest request
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

        // Chi cap nhat cac truong van ban duoc gui len.
        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Ten Property khong duoc de trong"
                );
            }
            property.setName(request.getName().trim());
        }

        if (request.getAddress() != null) {
            if (request.getAddress().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Dia chi Property khong duoc de trong"
                );
            }
            property.setAddress(request.getAddress().trim());
        }

        if (request.getPhone() != null) {
            property.setPhone(blankToNull(request.getPhone()));
        }

        if (request.getEmail() != null) {
            property.setEmail(blankToNull(request.getEmail()));
        }

        if (request.getDescription() != null) {
            property.setDescription(
                    blankToNull(request.getDescription())
            );
        }

        MultipartFile newImage = request.getImage();

        if (newImage != null && !newImage.isEmpty()) {
            String newImageKey = imageStorageService.upload(
                    tenant.getTenantId(),
                    newImage
            );

            property.setImage(newImageKey);
        }

        Property savedProperty = propertyRepository.saveAndFlush(property);

        return ViewPropertyResponse.from(savedProperty);
    }

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

    private String blankToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}