package com.ekr.saas_pms_with_chatBot;

import com.ekr.saas_pms_with_chatBot.config.StripeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(StripeProperties.class)
@EnableScheduling
public class SaasPmsWithChatBotApplication {
	public static void main(String[] args) {
		SpringApplication.run(SaasPmsWithChatBotApplication.class, args);
	}
}