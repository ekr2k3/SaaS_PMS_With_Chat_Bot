package com.ekr.saas_pms_with_chatBot;

import com.ekr.saas_pms_with_chatBot.config.MomoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MomoProperties.class)
public class SaasPmsWithChatBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(SaasPmsWithChatBotApplication.class, args);
	}

}
