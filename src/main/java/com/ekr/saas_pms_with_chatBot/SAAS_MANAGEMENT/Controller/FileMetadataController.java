package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.FileMetadata;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/files")
public class FileMetadataController {

    private final FileStorageService fileStorageService;

    public FileMetadataController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(
            value = "/pdf",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadPdf(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            FileMetadata metadata = fileStorageService.uploadPdf(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(metadata);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace(); // In lỗi chi tiết ra console
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Không thể upload PDF",
                            "error", e.getMessage()
                    ));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/pdf/{fileId}")
    public ResponseEntity<?> deletePdf(@PathVariable Long fileId) {
        try {
            fileStorageService.deletePdf(fileId);
            return ResponseEntity.noContent().build();

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể xóa PDF"));
        }
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pdf/saas/content")
    public ResponseEntity<String> readAllSaasPdfContents() {
        return ResponseEntity.ok(
                fileStorageService.readAllSaasPdfContents()
        );
    }
}
