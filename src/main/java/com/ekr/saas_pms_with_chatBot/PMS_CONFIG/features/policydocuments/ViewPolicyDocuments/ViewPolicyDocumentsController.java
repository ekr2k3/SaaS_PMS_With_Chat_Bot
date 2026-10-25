package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.policydocuments.ViewPolicyDocuments;


import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/policy-documents")
@PreAuthorize("hasRole('OWNER')")
public class ViewPolicyDocumentsController {

    private final ViewPolicyDocumentsService viewPolicyDocumentsService;

    public ViewPolicyDocumentsController(
            ViewPolicyDocumentsService viewPolicyDocumentsService
    ) {
        this.viewPolicyDocumentsService = viewPolicyDocumentsService;
    }

    /** Lấy danh sách tài liệu Policy & Rules của Property thuộc Owner hiện tại. */
    @GetMapping
    public ViewPolicyDocumentsResponse getPolicyDocuments(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return viewPolicyDocumentsService.execute(
                principal.getUsername(),
                propertyId
        );
    }
}
