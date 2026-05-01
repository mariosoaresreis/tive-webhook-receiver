package com.shiptrack.tive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
public class TiveWebhookReceiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiveWebhookReceiverApplication.class, args);
    }
}
