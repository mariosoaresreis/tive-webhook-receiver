package com.shiptrack.tive.kafka;

import com.shiptrack.tive.config.KafkaTopicsProperties;
import com.shiptrack.tive.model.TiveAlertPayload;
import com.shiptrack.tive.model.TiveWebhookPayload;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes Tive events to the correct Kafka topics.
 *
 * Design decisions:
 * - The message key is the entityName (trackerId) — ensures that events
 *   from the same tracker always go to the same partition (ordering preserved).
 * - Publishing is asynchronous (does not block the controller thread).
 * - On persistent failure, redirects to the DLQ topic.
 * - Metrics count successful publications and failures per topic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TiveEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties topics;
    private final MeterRegistry meterRegistry;

    private Counter positionsPublished;
    private Counter alertsPublished;
    private Counter publishFailures;

    @PostConstruct
    void initMetrics() {
        positionsPublished = Counter.builder("tive.webhook.published")
                .tag("topic", "positions")
                .description("Position events published to Kafka")
                .register(meterRegistry);

        alertsPublished = Counter.builder("tive.webhook.published")
                .tag("topic", "alerts")
                .description("Alerts published to Kafka")
                .register(meterRegistry);

        publishFailures = Counter.builder("tive.webhook.publish.failures")
                .description("Failures publishing to Kafka — sent to DLQ")
                .register(meterRegistry);
    }

    /**
     * Publishes a position/sensor event to the tive.positions topic.
     * Key = entityName to preserve ordering per tracker.
     */
    public void publishPosition(TiveWebhookPayload payload) {
        String key = payload.getEntityName();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topics.getPositions(), key, payload);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Position published. tracker={} offset={}",
                        key, result.getRecordMetadata().offset());
                positionsPublished.increment();
            } else {
                log.error("Failed to publish position. tracker={}", key, ex);
                sendToDlq(key, payload, "positions");
            }
        });
    }

    /**
     * Publishes an alert to the tive.alerts topic.
     */
    public void publishAlert(TiveAlertPayload payload) {
        String key = payload.getEntityName();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topics.getAlerts(), key, payload);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Alert published. tracker={} type={} offset={}",
                        key, payload.getAlertType(), result.getRecordMetadata().offset());
                alertsPublished.increment();
            } else {
                log.error("Failed to publish alert. tracker={} type={}", key, payload.getAlertType(), ex);
                sendToDlq(key, payload, "alerts");
            }
        });
    }

    /**
     * Dead Letter Queue: persistently failed events are sent here.
     * A separate job (DLQ reprocessor) attempts to republish them periodically.
     */
    private void sendToDlq(String key, Object payload, String originalTopic) {
        try {
            kafkaTemplate.send(topics.getDlq(), key, payload);
            publishFailures.increment();
            log.warn("Event sent to DLQ. tracker={} originalTopic={}", key, originalTopic);
        } catch (Exception dlqEx) {
            // If even the DLQ failed, log as ERROR to alert the ops team
            log.error("CRITICAL: failed to send to DLQ. tracker={}", key, dlqEx);
        }
    }
}
