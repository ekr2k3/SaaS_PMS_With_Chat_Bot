package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.RAG;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.FileMetadata;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.FileMetadataRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.FileStorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Đồng bộ PDF vào Qdrant với metadata dùng để cô lập phạm vi RAG. */
@Service
public class PolicyKnowledgeBaseService {

    public static final String SCOPE_SAAS = "SAAS";
    public static final String SCOPE_PROPERTY = "PROPERTY";

    private final VectorStore vectorStore;
    private final FileStorageService fileStorageService;
    private final FileMetadataRepository fileMetadataRepository;
    private final PropertyRepository propertyRepository;
    private final MinioClient minioClient;

    @Value("${minio.bucket_saas}")
    private String bucketName;

    public PolicyKnowledgeBaseService(
            VectorStore vectorStore,
            FileStorageService fileStorageService,
            FileMetadataRepository fileMetadataRepository,
            PropertyRepository propertyRepository,
            MinioClient minioClient) {
        this.vectorStore = vectorStore;
        this.fileStorageService = fileStorageService;
        this.fileMetadataRepository = fileMetadataRepository;
        this.propertyRepository = propertyRepository;
        this.minioClient = minioClient;
    }

    /** Tạo lại hai scope do ứng dụng quản lý, giữ nguyên các vector không thuộc scope này. */
    public synchronized void rebuildAll() {
        deleteScope(SCOPE_SAAS);
        deleteScope(SCOPE_PROPERTY);
        indexSaasDocuments();
        for (Property property : propertyRepository.findAll()) {
            reindexProperty(property.getPropertyId());
        }
    }

    @Transactional(readOnly = true)
    public synchronized void reindexProperty(Long propertyId) {
        deleteProperty(propertyId);
        List<FileMetadata> files = fileMetadataRepository
                .findByProperty_PropertyId(propertyId);
        List<Document> chunks = new ArrayList<>();

        for (FileMetadata file : files) {
            if (file.getAccount() == null || file.getProperty() == null
                    || !propertyId.equals(file.getProperty().getPropertyId())) {
                continue;
            }
            String text = readPdf(file);
            if (text.isBlank()) continue;

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("scope", SCOPE_PROPERTY);
            metadata.put("propertyId", propertyId.toString());
            metadata.put("accountId", file.getAccount().getAccountId().toString());
            metadata.put("fileId", file.getFileId().toString());
            metadata.put("fileName", file.getFileName());
            chunks.addAll(splitter().split(List.of(new Document(text, metadata))));
        }

        if (!chunks.isEmpty()) vectorStore.add(chunks);
    }

    public SearchRequest saasSearch(String question, int topK) {
        return SearchRequest.builder().query(question).topK(topK)
                .similarityThreshold(0.3)
                .filterExpression("scope == 'SAAS'").build();
    }

    public SearchRequest propertySearch(String question, Long propertyId, Long accountId, int topK) {
        return SearchRequest.builder().query(question).topK(topK)
                .similarityThreshold(0.3)
                .filterExpression("scope == 'PROPERTY' && propertyId == '" + propertyId
                        + "' && accountId == '" + accountId + "'")
                .build();
    }

    private void indexSaasDocuments() {
        String content = fileStorageService.readAllSaasPdfContents();
        if (content == null || content.isBlank()) return;
        Map<String, Object> metadata = Map.of("scope", SCOPE_SAAS);
        List<Document> chunks = splitter().split(List.of(new Document(content, metadata)));
        if (!chunks.isEmpty()) vectorStore.add(chunks);
    }

    private void deleteScope(String scope) {
        vectorStore.delete(new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder()
                .eq("scope", scope).build());
    }

    private void deleteProperty(Long propertyId) {
        vectorStore.delete(new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder()
                .and(new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder()
                                .eq("scope", SCOPE_PROPERTY),
                        new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder()
                                .eq("propertyId", propertyId.toString()))
                .build());
    }

    private String readPdf(FileMetadata metadata) {
        try (InputStream input = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName).object(metadata.getStoragePath()).build());
             PDDocument document = Loader.loadPDF(input.readAllBytes())) {
            return new PDFTextStripper().getText(document);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể đọc PDF: " + metadata.getFileName(), exception);
        }
    }

    private TokenTextSplitter splitter() {
        return TokenTextSplitter.builder().withChunkSize(800)
                .withMinChunkSizeChars(350).withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(10000).withKeepSeparator(true).build();
    }
}