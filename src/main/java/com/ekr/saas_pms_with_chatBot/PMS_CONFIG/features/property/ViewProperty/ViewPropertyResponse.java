package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.property.ViewProperty;


import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;

public record ViewPropertyResponse(
        Long propertyId,
        String name,
        String address,
        String phone,
        String email,
        String description,
        String image
) {
    public static ViewPropertyResponse from(Property property) {
        return new ViewPropertyResponse(
                property.getPropertyId(),
                property.getName(),
                property.getAddress(),
                property.getPhone(),
                property.getEmail(),
                property.getDescription(),
                property.getImage()
        );
    }
}
