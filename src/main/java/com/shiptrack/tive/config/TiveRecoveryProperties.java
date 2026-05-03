package com.shiptrack.tive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Data
@Component("tiveRecoveryProperties")
@ConfigurationProperties(prefix = "tive.recovery")
public class TiveRecoveryProperties {

    private boolean enabled = false;
    private Duration fixedDelay = Duration.ofMinutes(5);
    private Duration initialDelay = Duration.ofMinutes(1);
    private Duration lookback = Duration.ofMinutes(15);
    private Duration safetyOverlap = Duration.ofMinutes(2);
    private Duration lockTtl = Duration.ofMinutes(4);
    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private String fromParameter = "from";
    private String toParameter = "to";
    private String pageParameter = "page";
    private String pageSizeParameter = "pageSize";
    private int pageSize = 500;
    private int maxPages = 20;
    private Endpoint positions = new Endpoint();
    private Endpoint alerts = new Endpoint();

    @Data
    public static class Endpoint {
        private boolean enabled = true;
        private String path;
        private String responseItemsField;

        public boolean hasPath() {
            return StringUtils.hasText(path);
        }
    }
}


