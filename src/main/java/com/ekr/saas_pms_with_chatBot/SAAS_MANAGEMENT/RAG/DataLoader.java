package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.RAG;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DataLoader {

    private final PolicyKnowledgeBaseService policyKnowledgeBaseService;

    public DataLoader(PolicyKnowledgeBaseService policyKnowledgeBaseService) {
        this.policyKnowledgeBaseService = policyKnowledgeBaseService;
    }

    @PostConstruct
    void loadKnowledgeBases() {
        // Qdrant được xây dựng lại từ dữ liệu nguồn hiện có khi ứng dụng khởi động.
        policyKnowledgeBaseService.rebuildAll();
    }
}