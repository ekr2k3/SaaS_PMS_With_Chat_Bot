package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.chatbot.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Question không được để trống")
        String question
) {
}