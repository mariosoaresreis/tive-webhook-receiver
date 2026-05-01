package com.shiptrack.tive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tive.webhook")
public class TiveWebhookProperties {
    private String clientId;
    private String clientSecret;
    private long idempotencyTtlSeconds = 86400;
}
