package com.shiptrack.tive.controller;

import com.shiptrack.tive.kafka.TiveEventPublisher;
import com.shiptrack.tive.model.TiveAlertPayload;
import com.shiptrack.tive.model.TiveWebhookPayload;
import com.shiptrack.tive.service.IdempotencyService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Entry point for Tive Platform webhooks.
 *
 * Responsibilities (ONLY):
 *   1. Receive the payload
 *   2. Check idempotency (avoid duplicates)
 *   3. Route to the correct Kafka publisher
 *   4. Respond 200 IMMEDIATELY
 *
 * Does NOT: heavy parsing, external calls, persistence.
 * All of that is handled by downstream consumers.
 *
 * Endpoints:
 *   POST /webhooks/tive/positions  — device data (GPS + sensors)
 *   POST /webhooks/tive/alerts     — alerts (shock, temperature, geofence)
 *
 * Authentication: handled in WebhookAuthenticationFilter (before reaching here).
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/tive")
@RequiredArgsConstructor
public class TiveWebhookController {

    private final TiveEventPublisher publisher;
    private final IdempotencyService idempotencyService;
    private final MeterRegistry meterRegistry;

    private Counter receivedPositions;
    private Counter receivedAlerts;
    private Counter duplicatesIgnored;
    private Timer   webhookLatency;

    @PostConstruct
    void initMetrics() {
        receivedPositions = Counter.builder("tive.webhook.received")
                .tag("type", "position")
                .register(meterRegistry);

        receivedAlerts = Counter.builder("tive.webhook.received")
                .tag("type", "alert")
                .register(meterRegistry);

        duplicatesIgnored = Counter.builder("tive.webhook.duplicates")
                .description("Events ignored because they have already been processed")
                .register(meterRegistry);

        webhookLatency = Timer.builder("tive.webhook.latency")
                .description("Total webhook processing time (ms)")
                .register(meterRegistry);
    }

    // ─── Device Data (GPS + sensors) ─────────────────────────────────────────

    /**
     * Receives position and sensor data from all trackers.
     *
     * Flow:
     * 1. Validates idempotency via Redis (avoids processing the same event twice)
     * 2. Publishes asynchronously to Kafka (tive.positions)
     * 3. Returns 200 OK immediately — does not wait for Kafka confirmation
     *
     * Why not wait for Kafka?
     * - Tive has a short delivery timeout
     * - Blocking the controller thread wastes resources at 10k+ msg/s
     * - Kafka failures are handled in the async callback (sent to DLQ)
     */
    @PostMapping("/positions")
    public ResponseEntity<Void> receivePosition(@RequestBody TiveWebhookPayload payload) {
        return webhookLatency.record(() -> {
            String idempotencyKey = buildIdempotencyKey(payload.getEntityName(), payload.getEntryTimeEpoch());

            if (!idempotencyService.isNew(idempotencyKey)) {
                // Duplicate: respond 200 anyway (Tive doesn't need to know)
                duplicatesIgnored.increment();
                return ResponseEntity.ok().<Void>build();
            }

            log.info("Position received. tracker={} lat={} lon={}",
                    payload.getEntityName(),
                    payload.getLocation() != null ? payload.getLocation().getLatitude() : "?",
                    payload.getLocation() != null ? payload.getLocation().getLongitude() : "?");

            publisher.publishPosition(payload);
            receivedPositions.increment();

            // Immediate 200 OK — Kafka publishes asynchronously
            return ResponseEntity.ok().<Void>build();
        });
    }

    // ─── Alerts (shock, temperature, geofence) ────────────────────────────────

    /**
     * Receives critical event alerts from trackers.
     * Same idempotency flow, but publishes to the tive.alerts topic.
     */
    @PostMapping("/alerts")
    public ResponseEntity<Void> receiveAlert(@RequestBody TiveAlertPayload payload) {
        return webhookLatency.record(() -> {
            String idempotencyKey = buildAlertIdempotencyKey(payload);

            if (!idempotencyService.isNew(idempotencyKey)) {
                duplicatesIgnored.increment();
                return ResponseEntity.ok().<Void>build();
            }

            log.info("Alert received. tracker={} type={} value={}",
                    payload.getEntityName(),
                    payload.getAlertType(),
                    payload.getAlertDisplayValue());

            publisher.publishAlert(payload);
            receivedAlerts.increment();

            return ResponseEntity.ok().<Void>build();
        });
    }

    // ─── Health check for Tive to validate the endpoint ──────────────────────

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String buildIdempotencyKey(String trackerId, Long epochMs) {
        return trackerId + "::" + (epochMs != null ? epochMs : System.currentTimeMillis());
    }

    private String buildAlertIdempotencyKey(TiveAlertPayload payload) {
        String alertId = payload.getAlert() != null ? payload.getAlert().getAlertId() : payload.getAlertDate();
        return payload.getEntityName() + "::alert::" + alertId;
    }
}
