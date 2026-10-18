package com.ekr.saas_pms_with_chatBot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Autowired
    private ChatClient.Builder builder;

    @Bean
    public ChatClient chatClient() {
        ChatClient chatClient = builder.build();
        return chatClient;
    }

}