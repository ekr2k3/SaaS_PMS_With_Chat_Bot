package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class llmController {

    @Autowired
    ChatClient chatClient;

    @Autowired
    VectorStore vectorStore;

    // String có placeholder {documents}
    String systemPrompt = """
            You are a helpful assistant, 
            answering questions based on the given context 
            in the DOCUMENTS section and no prior knowledge.
            If the answer is not in the DOCUMENTS section, 
            then reply with "I don't know".
            
            DOCUMENTS:
            ----------------
            {documents}
            ----------------
            """;

    @GetMapping("/api/v1/chat/saas")
    public String RandomData(@RequestParam String question) {

        // 1. Tạo đối tượng SearchRequest: bao nhiêu vector (topK),
        //    mức độ giống tối thiểu (similarityThreshold), và query để so khớp
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(5)
                .similarityThreshold(0.5)
                .build();

        // 2. Dùng SearchRequest để search trong VectorStore
        //    Spring AI tự convert kết quả trả về thành List<Document>
        List<Document> relevantDocuments = vectorStore.similaritySearch(searchRequest);

        // 3. Convert List<Document> thành 1 String context duy nhất
        String documentsContext = relevantDocuments.stream()
                .map(Document::getText) // lấy nội dung text của từng Document
                .collect(Collectors.joining("\n"));

        // 4. Đưa documentsContext vào placeholder {documents} trong System Prompt
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        Message systemMessage = systemPromptTemplate.createMessage(
                Map.of("documents", documentsContext)
        );

        // 5. Dùng ChatClient để gửi message tới LLM
        //    - System Message: đã qua RAG, chỉ chứa dữ liệu liên quan (không gửi toàn bộ VectorStore)
        //    - User Message: câu hỏi gốc của user
        String response = chatClient.prompt()
                .messages(systemMessage)
                .user(question)
                .call()
                .content();

        return response;
    }

}
