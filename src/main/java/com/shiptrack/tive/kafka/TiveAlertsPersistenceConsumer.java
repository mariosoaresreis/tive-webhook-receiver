package com.shiptrack.tive.kafka;

import com.shiptrack.tive.model.TiveAlertPayload;
import com.shiptrack.tive.service.AlertPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tive.alert-persistence.enabled", havingValue = "true", matchIfMissing = true)
public class TiveAlertsPersistenceConsumer {

    private final AlertPersistenceService alertPersistenceService;

    @KafkaListener(
            topics = "${kafka.topics.alerts}",
            groupId = "${tive.alert-persistence.consumer-group:tive-alert-persistence}"
    )
    public void onAlert(
            TiveAlertPayload payload,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        alertPersistenceService.persistAlert(payload);
        log.debug("Persisted alert projection. key={} offset={} type={}", key, offset, payload.getAlertType());
    }
}


