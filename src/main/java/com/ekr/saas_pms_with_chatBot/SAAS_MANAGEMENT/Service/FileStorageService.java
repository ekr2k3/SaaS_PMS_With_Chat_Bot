package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;


import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.FileMetadata;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.FileMetadataRepository;
import io.minio.*;
import io.minio.errors.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class FileStorageService {

    private final MinioClient minioClient;
    private final FileMetadataRepository fileMetadataRepository;

    @Value("${minio.bucket_saas}")
    private String bucketName;

    public FileStorageService(
            MinioClient minioClient,
            FileMetadataRepository fileMetadataRepository
    ) {
        this.minioClient = minioClient;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    @Transactional
    public FileMetadata uploadPdf(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }

        originalName = originalName.replace("\\", "/");
        originalName = originalName.substring(
                originalName.lastIndexOf('/') + 1
        );

        if (!originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file PDF");
        }

        if (originalName.length() > 255) {
            throw new IllegalArgumentException("Tên file tối đa 255 ký tự");
        }

        // Kiểm tra chữ ký PDF, không chỉ dựa vào phần mở rộng.
        try (InputStream input = file.getInputStream()) {
            byte[] signature = input.readNBytes(5);

            if (!new String(signature).equals("%PDF-")) {
                throw new IllegalArgumentException("Nội dung file không phải PDF hợp lệ");
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Không đọc được file PDF", e);
        }

        String objectKey =
                "kb/" + UUID.randomUUID() + "_" + originalName;

        boolean uploaded = false;

        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
            }

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

            uploaded = true;

            FileMetadata metadata = FileMetadata.builder()
                    .fileName(originalName)
                    .fileType("PDF")
                    .fileSize(file.getSize())
                    .storagePath(objectKey)
                    .build();

            return fileMetadataRepository.saveAndFlush(metadata);

        }  catch (Exception e) {
        System.err.println("Lỗi upload PDF: " + e.getClass().getName());
        e.printStackTrace();

        if (uploaded) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .build()
                );
            } catch (Exception cleanupException) {
                e.addSuppressed(cleanupException);
            }
        }

        throw new IllegalStateException("Upload PDF thất bại", e);
    }
    }



    @Transactional
    public void deletePdf(Long fileId) {
        // 1. Tìm metadata trong DB
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Không tìm thấy file với ID: " + fileId
                        )
                );

        // 2. Xóa object khỏi MinIO
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(metadata.getStoragePath())
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Không thể xóa file khỏi MinIO", e
            );
        }

        // 3. Xóa metadata khỏi DB
        fileMetadataRepository.delete(metadata);
    }


    /**
     * Đọc toàn bộ nội dung PDF thuộc SaaS.
     * Quy ước: account_id IS NULL nghĩa là PDF thuộc SaaS.
     *
     * @return Một String chứa nội dung của tất cả PDF.
     */
    public String readAllSaasPdfContents() {

        List<FileMetadata> files =
                fileMetadataRepository.findByAccountIsNull();

        StringBuilder allContent = new StringBuilder();

        for (FileMetadata metadata : files) {
            try (
                    InputStream input = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(metadata.getStoragePath())
                                    .build()
                    );
                    PDDocument document =
                            Loader.loadPDF(input.readAllBytes())
            ) {
                String text = new PDFTextStripper().getText(document);

                if (!allContent.isEmpty()) {
                    allContent.append("\n\n");
                }

                allContent.append("===== ")
                        .append(metadata.getFileName())
                        .append(" =====\n")
                        .append(text);

            } catch (Exception e) {
                throw new IllegalStateException(
                        "Không thể đọc PDF: " + metadata.getFileName(),
                        e
                );
            }
        }

        return allContent.toString();
    }

}
