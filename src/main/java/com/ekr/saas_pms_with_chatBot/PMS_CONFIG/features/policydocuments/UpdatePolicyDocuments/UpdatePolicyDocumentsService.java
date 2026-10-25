package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.policydocuments.UpdatePolicyDocuments;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.FileMetadata;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.FileMetadataRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.RAG.PolicyKnowledgeBaseService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UpdatePolicyDocumentsService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final MinioClient minioClient;
    private final PolicyKnowledgeBaseService policyKnowledgeBaseService;

    @Value("${minio.bucket_saas}")
    private String bucketName;

    public UpdatePolicyDocumentsService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            FileMetadataRepository fileMetadataRepository,
            MinioClient minioClient,
            PolicyKnowledgeBaseService policyKnowledgeBaseService
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.minioClient = minioClient;
        this.policyKnowledgeBaseService = policyKnowledgeBaseService;
    }

    @Transactional
    public UpdatePolicyDocumentsResponse execute(
            String ownerEmail,
            Long propertyId,
            List<MultipartFile> requestedFiles,
            List<Long> requestedDeleteIds
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

        List<MultipartFile> files = requestedFiles == null
                ? List.of()
                : requestedFiles.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        List<Long> deleteIds = requestedDeleteIds == null
                ? List.of()
                : requestedDeleteIds;

        if (files.isEmpty() && deleteIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khong co thay doi nao de cap nhat"
            );
        }

        Set<Long> uniqueDeleteIds = new HashSet<>(deleteIds);

        if (uniqueDeleteIds.size() != deleteIds.size()
                || uniqueDeleteIds.contains(null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "deleteFileIds khong duoc trung lap hoac chua gia tri null"
            );
        }

        // Xác minh tất cả ID xóa trước khi thay đổi lưu trữ.
        List<FileMetadata> filesToDelete = new ArrayList<>();

        for (Long fileId : uniqueDeleteIds) {
            FileMetadata metadata = fileMetadataRepository.findById(fileId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Khong tim thay tai lieu: " + fileId
                    ));

            if (metadata.getProperty() == null
                    || !propertyId.equals(
                    metadata.getProperty().getPropertyId()
            )
                    || metadata.getAccount() == null
                    || !account.getAccountId().equals(
                    metadata.getAccount().getAccountId()
            )) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tai lieu khong thuoc Property hien tai: " + fileId
                );
            }

            filesToDelete.add(metadata);
        }

        // Kiểm tra file mới trước khi upload.
        for (MultipartFile file : files) {
            validatePdf(file);
        }

        List<String> uploadedObjectKeys = new ArrayList<>();
        List<FileMetadata> newMetadata = new ArrayList<>();

        try {
            ensureBucketExists();

            for (MultipartFile file : files) {
                String fileName = safeFileName(
                        file.getOriginalFilename()
                );

                String objectKey = "PMS/"
                        + tenant.getTenantId()
                        + "/"
                        + property.getPropertyId()
                        + "/"
                        + UUID.randomUUID()
                        + "_"
                        + fileName;

                try (InputStream input = file.getInputStream()) {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectKey)
                                    .stream(input, file.getSize(), -1)
                                    .contentType("application/pdf")
                                    .build()
                    );
                }

                uploadedObjectKeys.add(objectKey);

                newMetadata.add(FileMetadata.builder()
                        .fileName(fileName)
                        .fileType("PDF")
                        .fileSize(file.getSize())
                        .storagePath(objectKey)
                        .account(account)
                        .property(property)
                        .build());
            }

            if (!newMetadata.isEmpty()) {
                fileMetadataRepository.saveAllAndFlush(newMetadata);
            }

            // Xóa metadata trong transaction.
            // Object cũ chỉ được dọn sau khi transaction commit.
            if (!filesToDelete.isEmpty()) {
                fileMetadataRepository.deleteAll(filesToDelete);
                fileMetadataRepository.flush();

                registerObjectCleanupAfterCommit(
                        filesToDelete.stream()
                                .map(FileMetadata::getStoragePath)
                                .toList()
                );
            }

            // Nếu transaction rollback, dọn các object mới đã upload.
            registerNewObjectCleanupAfterCompletion(
                    uploadedObjectKeys
            );
            registerKnowledgeBaseRefreshAfterCommit(property.getPropertyId());

            return new UpdatePolicyDocumentsResponse(
                    property.getPropertyId(),
                    "Cap nhat tai lieu thanh cong",
                    newMetadata.size(),
                    filesToDelete.size()
            );

        } catch (ResponseStatusException e) {
            cleanupUploadedObjects(uploadedObjectKeys);
            throw e;
        } catch (Exception e) {
            cleanupUploadedObjects(uploadedObjectKeys);
            throw new IllegalStateException(
                    "Cap nhat Policy Documents that bai",
                    e
            );
        }
    }


    private void registerKnowledgeBaseRefreshAfterCommit(Long propertyId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            policyKnowledgeBaseService.reindexProperty(propertyId);
                        } catch (Exception exception) {
                            // Metadata/MinIO đã commit; lần khởi động sau sẽ rebuild lại KB.
                            System.err.println("Khong the dong bo Knowledge Base cho Property "
                                    + propertyId + ": " + exception.getMessage());
                        }
                    }
                }
        );
    }

    private void validatePdf(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File khong duoc de trong"
            );
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ten file khong hop le"
            );
        }

        if (!originalName.toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chi chap nhan file PDF"
            );
        }

        if (safeFileName(originalName).length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ten file toi da 255 ky tu"
            );
        }

        try (InputStream input = file.getInputStream()) {
            String signature = new String(
                    input.readNBytes(5),
                    java.nio.charset.StandardCharsets.US_ASCII
            );

            if (!"%PDF-".equals(signature)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Noi dung file khong phai PDF hop le"
                );
            }

        } catch (java.io.IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khong doc duoc file PDF",
                    e
            );
        }
    }

    private String safeFileName(String originalName) {
        String normalized = originalName.replace("\\", "/");

        return normalized.substring(
                normalized.lastIndexOf('/') + 1
        );
    }

    private void ensureBucketExists() throws Exception {
        if (!minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        )) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
        }
    }

    private void cleanupUploadedObjects(List<String> objectKeys) {
        for (String objectKey : objectKeys) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .build()
                );
            } catch (Exception cleanupException) {
                System.err.println(
                        "Khong the don object MinIO: " + objectKey
                );
            }
        }
    }

    private void registerNewObjectCleanupAfterCompletion(
            List<String> objectKeys
    ) {
        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            cleanupUploadedObjects(objectKeys);
                        }
                    }
                }
        );
    }

    private void registerObjectCleanupAfterCommit(
            List<String> objectKeys
    ) {
        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        cleanupUploadedObjects(objectKeys);
                    }
                }
        );
    }
}