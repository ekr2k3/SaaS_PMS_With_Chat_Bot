package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.RAG;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataLoader {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private FileStorageService fileStorageService;

    @PostConstruct
    void loadSentencesIntoVectorStore(){ // load Câu vào trong Vector Store

        // 0 . Log check
        System.out.println("Run rag loader");


// 1. Lấy nội dung PDF
        String content = fileStorageService.readAllSaasPdfContents();

        if (content == null || content.isBlank()) {
            return;
        }

        // 2. Chuyển thành Document
        Document document = new Document(content);

        // 3. Chia thành các chunk nhỏ
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = splitter.split(List.of(document));

        // 4. Lưu các chunk vào Qdrant
        vectorStore.add(chunks);
    }

}
