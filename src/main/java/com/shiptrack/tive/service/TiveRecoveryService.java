package com.shiptrack.tive.service;

import com.shiptrack.tive.config.TiveRecoveryProperties;
import com.shiptrack.tive.kafka.TiveEventPublisher;
import com.shiptrack.tive.model.TiveAlertPayload;
import com.shiptrack.tive.model.TiveWebhookPayload;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tive.recovery.enabled", havingValue = "true")
public class TiveRecoveryService {

    private final TiveRecoveryProperties properties;
    private final TiveRecoveryClient recoveryClient;
    private final TiveRecoveryCursorService cursorService;
    private final IdempotencyService idempotencyService;
    private final TiveEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    private Counter positionsRepublished;
    private Counter alertsRepublished;
    private Counter duplicatesIgnored;
    private Counter positionsFailures;
    private Counter alertsFailures;
    private Timer pollTimer;

    @PostConstruct
    void initMetrics() {
        positionsRepublished = Counter.builder("tive.recovery.republished")
                .tag("stream", "positions")
                .description("Recovered position events republished to Kafka")
                .register(meterRegistry);
        alertsRepublished = Counter.builder("tive.recovery.republished")
                .tag("stream", "alerts")
                .description("Recovered alert events republished to Kafka")
                .register(meterRegistry);
        duplicatesIgnored = Counter.builder("tive.recovery.duplicates")
                .description("Recovered events ignored because they were already processed")
                .register(meterRegistry);
        positionsFailures = Counter.builder("tive.recovery.failures")
                .tag("stream", "positions")
                .description("Failed recovery polls for Tive position events")
                .register(meterRegistry);
        alertsFailures = Counter.builder("tive.recovery.failures")
                .tag("stream", "alerts")
                .description("Failed recovery polls for Tive alert events")
                .register(meterRegistry);
        pollTimer = Timer.builder("tive.recovery.poll.duration")
                .description("Execution time for one Tive recovery polling cycle")
                .register(meterRegistry);
    }

    @Scheduled(
            initialDelayString = "#{@tiveRecoveryProperties.initialDelay.toMillis()}",
            fixedDelayString = "#{@tiveRecoveryProperties.fixedDelay.toMillis()}"
    )
    public void recoverMissedEvents() {
        Optional<String> lockToken = cursorService.acquireRunLock(properties.getLockTtl());
        if (lockToken.isEmpty()) {
            log.debug("Skipping Tive recovery poll because another instance is already running");
            return;
        }

        try {
            pollTimer.record(this::runRecoveryCycle);
        } finally {
            cursorService.releaseRunLock(lockToken.get());
        }
    }

    private void runRecoveryCycle() {
        recoverPositions();
        recoverAlerts();
    }

    private void recoverPositions() {
        if (!properties.getPositions().isEnabled() || !properties.getPositions().hasPath()) {
            return;
        }

        Instant toExclusive = Instant.now();
        Instant fromInclusive = resolveFrom(TiveRecoveryCursorService.RecoveryStream.POSITIONS, toExclusive);

        try {
            List<TiveWebhookPayload> payloads = recoveryClient.fetchPositions(fromInclusive, toExclusive);
            int republished = 0;
            int duplicates = 0;

            for (TiveWebhookPayload payload : payloads) {
                if (!StringUtils.hasText(payload.getEntityName())) {
                    log.debug("Ignoring recovered position without tracker identifier");
                    continue;
                }

                if (idempotencyService.isNew(TiveEventKeys.positionKey(payload))) {
                    eventPublisher.publishPosition(payload);
                    positionsRepublished.increment();
                    republished++;
                } else {
                    duplicatesIgnored.increment();
                    duplicates++;
                }
            }

            cursorService.updateCursor(TiveRecoveryCursorService.RecoveryStream.POSITIONS, toExclusive);
            log.info("Tive recovery cycle for positions completed. fetched={} republished={} duplicates={} from={} to={}",
                    payloads.size(), republished, duplicates, fromInclusive, toExclusive);
        } catch (Exception ex) {
            positionsFailures.increment();
            log.error("Failed to recover missed Tive position events. from={} to={}", fromInclusive, toExclusive, ex);
        }
    }

    private void recoverAlerts() {
        if (!properties.getAlerts().isEnabled() || !properties.getAlerts().hasPath()) {
            return;
        }

        Instant toExclusive = Instant.now();
        Instant fromInclusive = resolveFrom(TiveRecoveryCursorService.RecoveryStream.ALERTS, toExclusive);

        try {
            List<TiveAlertPayload> payloads = recoveryClient.fetchAlerts(fromInclusive, toExclusive);
            int republished = 0;
            int duplicates = 0;

            for (TiveAlertPayload payload : payloads) {
                if (!StringUtils.hasText(payload.getEntityName())) {
                    log.debug("Ignoring recovered alert without tracker identifier");
                    continue;
                }

                if (idempotencyService.isNew(TiveEventKeys.alertKey(payload))) {
                    eventPublisher.publishAlert(payload);
                    alertsRepublished.increment();
                    republished++;
                } else {
                    duplicatesIgnored.increment();
                    duplicates++;
                }
            }

            cursorService.updateCursor(TiveRecoveryCursorService.RecoveryStream.ALERTS, toExclusive);
            log.info("Tive recovery cycle for alerts completed. fetched={} republished={} duplicates={} from={} to={}",
                    payloads.size(), republished, duplicates, fromInclusive, toExclusive);
        } catch (Exception ex) {
            alertsFailures.increment();
            log.error("Failed to recover missed Tive alert events. from={} to={}", fromInclusive, toExclusive, ex);
        }
    }

    private Instant resolveFrom(TiveRecoveryCursorService.RecoveryStream stream, Instant toExclusive) {
        return cursorService.readCursor(stream)
                .map(cursor -> cursor.minus(properties.getSafetyOverlap()))
                .orElseGet(() -> toExclusive.minus(properties.getLookback()));
    }
}


