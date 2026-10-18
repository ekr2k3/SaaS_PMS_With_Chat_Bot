
package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileMetadataRepository
        extends JpaRepository<FileMetadata, Long> {

    List<FileMetadata> findByAccountIsNull();
}