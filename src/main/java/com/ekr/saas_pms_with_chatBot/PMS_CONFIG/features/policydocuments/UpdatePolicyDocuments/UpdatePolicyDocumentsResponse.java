package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.policydocuments.UpdatePolicyDocuments;

public record UpdatePolicyDocumentsResponse(
        Long propertyId,
        String message,
        int addedCount,
        int deletedCount
) {
}
