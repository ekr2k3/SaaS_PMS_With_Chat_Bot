package com.ekr.saas_pms_with_chatBot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "momo")
@Getter
@Setter
public class MomoProperties {

    private String partnerCode;

    private String accessKey;

    private String secretKey;

    private String endpoint;

    private String redirectUrl;

    private String ipnUrl;
}
