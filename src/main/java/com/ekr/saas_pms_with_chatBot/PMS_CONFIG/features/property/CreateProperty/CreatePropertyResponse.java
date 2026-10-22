package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.property.CreateProperty;


public record CreatePropertyResponse(
        Long propertyId,
        String name,
        String address,
        String phone,
        String email,
        String description,
        String image
) {
}
