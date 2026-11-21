package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Hỗ trợ LocalDate, LocalDateTime, LocalTime...
        objectMapper.registerModule(new JavaTimeModule());

        // Serialize LocalDateTime thành chuỗi ISO-8601
        objectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );

        return objectMapper;
    }
}