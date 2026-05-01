package com.shiptrack.tive.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Autowired
    private KafkaTopicsProperties topics;

    /**
     * Automatically creates topics on the broker if they do not exist.
     * In production, prefer managing topics via Terraform or Helm.
     */
    @Bean
    public NewTopic positionsTopic() {
        return TopicBuilder.name(topics.getPositions())
                .partitions(12)   // 1 partition per tracker group
                .replicas(3)
                .build();
    }

    @Bean
    public NewTopic alertsTopic() {
        return TopicBuilder.name(topics.getAlerts())
                .partitions(6)
                .replicas(3)
                .build();
    }

    @Bean
    public NewTopic sensorsTopic() {
        return TopicBuilder.name(topics.getSensors())
                .partitions(12)
                .replicas(3)
                .build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(topics.getDlq())
                .partitions(3)
                .replicas(3)
                .build();
    }
}
