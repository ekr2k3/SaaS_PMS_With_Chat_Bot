package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Tool.SupportTicketTool;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class llmController {

    @Autowired
    ChatClient chatClient;

    @Autowired
    VectorStore vectorStore;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    SupportTicketTool supportTicketTool;

    String systemPrompt = """
            You are a helpful assistant,
            answering questions based on the given context
            in the DOCUMENTS section and no prior knowledge.
            Answer using the DOCUMENTS section only.
            If the documents do not contain enough information to answer the user's question,
            call the createSupportTicket tool with the user's original question.
            Do not invent an answer when the documents are insufficient.

            DOCUMENTS:
            ----------------
            {documents}
            ----------------
            """;

    @GetMapping("/api/v1/chat/saas")
    public String RandomData(@RequestParam String question) {

        // 1. Lấy email từ principal sau khi JWT đã được xác thực.
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chưa xác thực người dùng"
            );
        }

        // 2. Từ email, truy vấn Account để lấy accountId.
        String email = authentication.getName();
        Account account = accountRepository.findByEmail(email);

        if (account == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Người dùng hiện tại không thuộc Account Owner"
            );
        }

        Long accountId = account.getAccountId();

        // 3. Tìm kiếm tài liệu liên quan trong VectorStore.
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(5)
                .similarityThreshold(0.5)
                .build();

        List<Document> relevantDocuments =
                vectorStore.similaritySearch(searchRequest);

        // 4. Ghép nội dung tài liệu thành context cho RAG.
        String documentsContext = relevantDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // 5. Đưa context vào System Prompt.
        SystemPromptTemplate systemPromptTemplate =
                new SystemPromptTemplate(systemPrompt);

        Message systemMessage = systemPromptTemplate.createMessage(
                Map.of("documents", documentsContext)
        );

        // 6. Gọi ChatClient và truyền accountId qua ToolContext.
        String response = chatClient.prompt()
                .messages(systemMessage)
                .user(question)
                .tools(supportTicketTool)
                .toolContext(Map.of("accountId", accountId))
                .call()
                .content();

        return response;
    }
}