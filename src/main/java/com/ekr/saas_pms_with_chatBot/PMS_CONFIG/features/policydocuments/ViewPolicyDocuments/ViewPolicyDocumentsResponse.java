package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.policydocuments.ViewPolicyDocuments;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.FileMetadata;

import java.util.List;

public record ViewPolicyDocumentsResponse(
        Long propertyId,
        List<DocumentItem> documents
) {

    public record DocumentItem(
            Long fileId,
            String fileName,
            String fileType,
            Long fileSize
    ) {
        public static DocumentItem from(FileMetadata metadata) {
            return new DocumentItem(
                    metadata.getFileId(),
                    metadata.getFileName(),
                    metadata.getFileType(),
                    metadata.getFileSize()
            );
        }
    }
}