package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.infrastructure.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

@Service
public class PropertyImageStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket_saas}")
    private String bucketName;

    public PropertyImageStorageService(
            MinioClient minioClient
    ) {
        this.minioClient = minioClient;
    }

    public String upload(
            Long tenantId,
            MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ảnh không được để trống"
            );
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên ảnh không hợp lệ"
            );
        }

        String normalizedName = originalName.replace("\\", "/");
        normalizedName = normalizedName.substring(
                normalizedName.lastIndexOf('/') + 1
        );

        int dotIndex = normalizedName.lastIndexOf('.');

        if (dotIndex < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ảnh phải có phần mở rộng hợp lệ"
            );
        }

        String extension = normalizedName
                .substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);

        String contentType = file.getContentType();

        boolean supportedImage =
                ("jpg".equals(extension) || "jpeg".equals(extension))
                        && "image/jpeg".equalsIgnoreCase(contentType)
                        || "png".equals(extension)
                        && "image/png".equalsIgnoreCase(contentType)
                        || "webp".equals(extension)
                        && "image/webp".equalsIgnoreCase(contentType);

        if (!supportedImage) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ chấp nhận ảnh JPG, JPEG, PNG hoặc WEBP"
            );
        }

        String objectKey = "properties/"
                + tenantId
                + "/"
                + UUID.randomUUID()
                + "."
                + extension;

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
                                .contentType(contentType)
                                .build()
                );
            }

            return objectKey;

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Không thể upload ảnh Property lên MinIO",
                    ex
            );
        }
    }

    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Không thể xóa ảnh Property khỏi MinIO",
                    ex
            );
        }
    }
}
