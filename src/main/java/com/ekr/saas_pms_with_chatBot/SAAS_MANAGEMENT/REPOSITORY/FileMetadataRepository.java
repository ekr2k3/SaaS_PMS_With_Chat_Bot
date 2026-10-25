package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileMetadataRepository
        extends JpaRepository<FileMetadata, Long> {

    /** Tài liệu cấp SaaS Admin: không gắn Account hoặc Property. */
    List<FileMetadata> findByAccountIsNullAndPropertyIsNull();

    /** Tài liệu được gắn với một Property cụ thể. */
    List<FileMetadata> findByProperty_PropertyId(Long propertyId);

    /** Tài liệu của Property, giới hạn thêm theo Account sở hữu. */
    List<FileMetadata> findByProperty_PropertyIdAndAccount_AccountId(
            Long propertyId,
            Long accountId
    );

    /** Giữ lại để tương thích với các caller cũ; ưu tiên method có cả PropertyIsNull. */
    List<FileMetadata> findByAccountIsNull();
}