package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.chatbot;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.chatbot.dto.ChatRequest;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.RAG.PropertyChatService;
import com.ekr.saas_pms_with_chatBot.filter.PmsEmployeePrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/pms/chat")
public class PmsChatController {

    private final PropertyRepository propertyRepository;
    private final PropertyChatService propertyChatService;

    public PmsChatController(
            PropertyRepository propertyRepository,
            PropertyChatService propertyChatService) {
        this.propertyRepository = propertyRepository;
        this.propertyChatService = propertyChatService;
    }

    @PostMapping
    public String chat(
            @AuthenticationPrincipal PmsEmployeePrincipal principal,
            @Valid @RequestBody ChatRequest request) {

        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "API này chỉ dành cho nhân viên PMS");
        }

        if (principal.propertyId() == null || principal.tenantId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Tài khoản PMS chưa được gán Property hợp lệ");
        }

        Property property = propertyRepository
                .findWithTenantAndAccountByPropertyIdAndTenant_TenantId(
                        principal.propertyId(),
                        principal.tenantId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Không có quyền truy cập Property này"));

        Tenant tenant = property.getTenant();
        Account account = tenant == null ? null : tenant.getAccount();

        if (tenant == null
                || tenant.getStatus() != Tenant.TenantStatus.ACTIVE
                || account == null
                || account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Property hiện không khả dụng");
        }

        return propertyChatService.ask(
                request.question(),
                property.getPropertyId(),
                account.getAccountId()
        );
    }
}