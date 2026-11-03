package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.chatbot;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.chatbot.dto.ChatRequest;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.RAG.PropertyChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/properties/{propertyId}/chat")
public class PublicChatController {

    private final PropertyRepository propertyRepository;
    private final PropertyChatService propertyChatService;

    public PublicChatController(
            PropertyRepository propertyRepository,
            PropertyChatService propertyChatService) {
        this.propertyRepository = propertyRepository;
        this.propertyChatService = propertyChatService;
    }

    @PostMapping
    public String chat(
            @PathVariable Long propertyId,
            @Valid @RequestBody ChatRequest request) {

        Property property = propertyRepository
                .findWithTenantAndAccountByPropertyId(propertyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy Property"));

        Tenant tenant = property.getTenant();
        Account account = tenant == null ? null : tenant.getAccount();

        if (tenant == null || tenant.getStatus() != Tenant.TenantStatus.ACTIVE
                || account == null || account.getStatus() != Account.AccountStatus.ACTIVE) {
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