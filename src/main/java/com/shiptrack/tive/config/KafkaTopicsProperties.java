package com.shiptrack.tive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicsProperties {
    private String positions = "tive.positions";
    private String alerts    = "tive.alerts";
    private String sensors   = "tive.sensors";
    private String dlq       = "tive.dlq";
}
