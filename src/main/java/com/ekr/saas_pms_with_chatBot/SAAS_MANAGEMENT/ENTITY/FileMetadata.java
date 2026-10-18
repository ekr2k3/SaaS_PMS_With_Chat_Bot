
package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "FILE_METADATA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
            name = "account_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "fk_file_metadata_account")
    )
    private Account account;
}