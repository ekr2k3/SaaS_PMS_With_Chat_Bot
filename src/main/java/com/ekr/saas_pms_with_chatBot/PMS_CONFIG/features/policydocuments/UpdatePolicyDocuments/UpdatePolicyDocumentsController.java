package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.policydocuments.UpdatePolicyDocuments;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/policy-documents")
@PreAuthorize("hasRole('OWNER')")
public class UpdatePolicyDocumentsController {

    private final UpdatePolicyDocumentsService updatePolicyDocumentsService;

    public UpdatePolicyDocumentsController(
            UpdatePolicyDocumentsService updatePolicyDocumentsService
    ) {
        this.updatePolicyDocumentsService = updatePolicyDocumentsService;
    }

    /**
     * Cập nhật danh sách tài liệu của Property trong một request
     * multipart/form-data.
     */
    @PutMapping(consumes = "multipart/form-data")
    public UpdatePolicyDocumentsResponse updatePolicyDocuments(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(value = "files", required = false)
            List<MultipartFile> files,
            @RequestParam(value = "deleteFileIds", required = false)
            List<Long> deleteFileIds
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return updatePolicyDocumentsService.execute(
                principal.getUsername(),
                propertyId,
                files,
                deleteFileIds
        );
    }
}