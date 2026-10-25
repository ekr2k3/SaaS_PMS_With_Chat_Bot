package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.RAG.PolicyKnowledgeBaseService;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Tool.SupportTicketTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class llmController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final SupportTicketTool supportTicketTool;
    private final PolicyKnowledgeBaseService policyKnowledgeBaseService;

    public llmController(
            ChatClient chatClient,
            VectorStore vectorStore,
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            SupportTicketTool supportTicketTool,
            PolicyKnowledgeBaseService policyKnowledgeBaseService) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.supportTicketTool = supportTicketTool;
        this.policyKnowledgeBaseService = policyKnowledgeBaseService;
    }

    private final String systemPrompt = """
            You are a helpful assistant, answering questions based only on the DOCUMENTS section.
            Use only the provided documents. If they do not contain enough information, call
            createSupportTicket with the user's original question. Do not invent an answer.

            DOCUMENTS:
            ----------------
            {documents}
            ----------------
            """;

    /** Chat hỏi đáp tài liệu cấp SaaS; không được truy xuất tài liệu Property. */
    @GetMapping("/api/v1/chat/saas")
    public String chatSaas(@RequestParam String question) {
        Account account = requireCurrentAccount();
        return answer(question,
                policyKnowledgeBaseService.saasSearch(question, 5),
                Map.of("accountId", account.getAccountId()));
    }

    /** Chat hỏi đáp Policy của một Property thuộc Tenant của Owner đang đăng nhập. */
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/api/properties/{propertyId}/chat")
    public String chatProperty(
            @PathVariable Long propertyId,
            @RequestParam String question) {
        Account account = requireCurrentAccount();
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Tài khoản Owner không hoạt động");
        }

        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Owner chưa có Tenant"));
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(propertyId, tenant.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy Property"));

        SearchRequest request = policyKnowledgeBaseService
                .propertySearch(question, property.getPropertyId(), account.getAccountId(), 5);
        return answer(question, request, Map.of("accountId", account.getAccountId(),
                "propertyId", property.getPropertyId()));
    }

    private Account requireCurrentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Chưa xác thực người dùng");
        }
        Account account = accountRepository.findByEmail(authentication.getName());
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Người dùng hiện tại không thuộc Account Owner");
        }
        return account;
    }

    private String answer(String question, SearchRequest searchRequest,
                          Map<String, Object> toolContext) {
        List<Document> relevantDocuments = vectorStore.similaritySearch(searchRequest);
        
        // Print các vector tìm được để debug
        System.out.println("=== RAG Search Results ===");
        System.out.println("Question: " + question);
        System.out.println("Found " + relevantDocuments.size() + " documents");
        for (int i = 0; i < relevantDocuments.size(); i++) {
            Document doc = relevantDocuments.get(i);
            System.out.println("\n--- Document " + (i + 1) + " ---");
            System.out.println("Content: " + doc.getText().substring(0, Math.min(200, doc.getText().length())));
            System.out.println("Metadata: " + doc.getMetadata());
        }
        System.out.println("========================");
        
        String documentsContext = relevantDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        Message systemMessage = new SystemPromptTemplate(systemPrompt)
                .createMessage(Map.of("documents", documentsContext));

        return chatClient.prompt()
                .messages(systemMessage)
                .user(question)
                .tools(supportTicketTool)
                .toolContext(toolContext)
                .call()
                .content();
    }
}