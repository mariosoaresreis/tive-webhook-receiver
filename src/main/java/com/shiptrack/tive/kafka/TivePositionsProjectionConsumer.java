package com.shiptrack.tive.kafka;

import com.shiptrack.tive.model.TiveWebhookPayload;
import com.shiptrack.tive.service.TrackerPositionStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TivePositionsProjectionConsumer {

    private final TrackerPositionStateService trackerPositionStateService;

    @KafkaListener(
            topics = "${kafka.topics.positions}",
            groupId = "${tive.position-state.consumer-group:tive-position-state}"
    )
    public void onPosition(
            TiveWebhookPayload payload,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        trackerPositionStateService.upsertLatest(payload);
        log.debug("Projected latest position. key={} partition={} offset={}", key, partition, offset);
    }
}
