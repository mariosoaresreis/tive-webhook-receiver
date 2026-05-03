package com.shiptrack.tive.service;

import com.shiptrack.tive.config.TiveRecoveryProperties;
import com.shiptrack.tive.kafka.TiveEventPublisher;
import com.shiptrack.tive.model.TiveAlertPayload;
import com.shiptrack.tive.model.TiveWebhookPayload;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TiveRecoveryServiceTest {

    @Mock
    private TiveRecoveryClient recoveryClient;

    @Mock
    private TiveRecoveryCursorService cursorService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private TiveEventPublisher eventPublisher;

    private TiveRecoveryProperties properties;
    private TiveRecoveryService service;

    @BeforeEach
    void setUp() {
        properties = new TiveRecoveryProperties();
        properties.setEnabled(true);
        properties.setLookback(Duration.ofMinutes(15));
        properties.setSafetyOverlap(Duration.ofMinutes(2));
        properties.getPositions().setPath("/positions");
        properties.getAlerts().setPath("/alerts");

        service = new TiveRecoveryService(
                properties,
                recoveryClient,
                cursorService,
                idempotencyService,
                eventPublisher,
                new SimpleMeterRegistry()
        );
        service.initMetrics();
    }

    @Test
    void shouldRepublishRecoveredEventsAndAdvanceCursors() {
        TiveWebhookPayload position = positionPayload("TRACKER-001", 1712345678000L);
        TiveAlertPayload alert = alertPayload("TRACKER-001", "alert-1");

        when(cursorService.acquireRunLock(any())).thenReturn(Optional.of("lock-token"));
        when(recoveryClient.fetchPositions(any(), any())).thenReturn(List.of(position));
        when(recoveryClient.fetchAlerts(any(), any())).thenReturn(List.of(alert));
        when(idempotencyService.isNew(TiveEventKeys.positionKey(position))).thenReturn(true);
        when(idempotencyService.isNew(TiveEventKeys.alertKey(alert))).thenReturn(true);

        service.recoverMissedEvents();

        verify(eventPublisher).publishPosition(position);
        verify(eventPublisher).publishAlert(alert);
        verify(cursorService).updateCursor(eq(TiveRecoveryCursorService.RecoveryStream.POSITIONS), any(Instant.class));
        verify(cursorService).updateCursor(eq(TiveRecoveryCursorService.RecoveryStream.ALERTS), any(Instant.class));
        verify(cursorService).releaseRunLock("lock-token");
    }

    @Test
    void shouldSkipRunWhenLockIsNotAvailable() {
        when(cursorService.acquireRunLock(any())).thenReturn(Optional.empty());

        service.recoverMissedEvents();

        verifyNoInteractions(recoveryClient, idempotencyService, eventPublisher);
        verify(cursorService, never()).releaseRunLock(any());
    }

    @Test
    void shouldIgnoreDuplicatesReturnedByRecoveryApi() {
        TiveWebhookPayload position = positionPayload("TRACKER-002", 1712345679000L);

        when(cursorService.acquireRunLock(any())).thenReturn(Optional.of("lock-token"));
        when(recoveryClient.fetchPositions(any(), any())).thenReturn(List.of(position));
        when(recoveryClient.fetchAlerts(any(), any())).thenReturn(List.of());
        when(idempotencyService.isNew(TiveEventKeys.positionKey(position))).thenReturn(false);

        service.recoverMissedEvents();

        verify(eventPublisher, never()).publishPosition(any());
        verify(cursorService).updateCursor(eq(TiveRecoveryCursorService.RecoveryStream.POSITIONS), any(Instant.class));
        verify(cursorService).releaseRunLock("lock-token");
    }

    @Test
    void shouldContinueWithAlertsWhenPositionRecoveryFails() {
        TiveAlertPayload alert = alertPayload("TRACKER-003", "alert-3");

        when(cursorService.acquireRunLock(any())).thenReturn(Optional.of("lock-token"));
        when(recoveryClient.fetchPositions(any(), any())).thenThrow(new IllegalStateException("boom"));
        when(recoveryClient.fetchAlerts(any(), any())).thenReturn(List.of(alert));
        when(idempotencyService.isNew(TiveEventKeys.alertKey(alert))).thenReturn(true);

        service.recoverMissedEvents();

        verify(eventPublisher).publishAlert(alert);
        verify(cursorService, never()).updateCursor(eq(TiveRecoveryCursorService.RecoveryStream.POSITIONS), any());
        verify(cursorService).updateCursor(eq(TiveRecoveryCursorService.RecoveryStream.ALERTS), any(Instant.class));
        verify(cursorService).releaseRunLock("lock-token");
    }

    private TiveWebhookPayload positionPayload(String trackerId, long entryTimeEpoch) {
        TiveWebhookPayload payload = new TiveWebhookPayload();
        payload.setEntityName(trackerId);
        payload.setEntryTimeEpoch(entryTimeEpoch);
        payload.setEntryTimeUtc("2026-05-03T10:15:30Z");
        return payload;
    }

    private TiveAlertPayload alertPayload(String trackerId, String alertId) {
        TiveAlertPayload payload = new TiveAlertPayload();
        payload.setEntityName(trackerId);
        payload.setAlertDate("2026-05-03T10:15:30Z");
        payload.setAlertType("TEMPERATURE");

        TiveAlertPayload.AlertDetail detail = new TiveAlertPayload.AlertDetail();
        detail.setAlertId(alertId);
        payload.setAlert(detail);
        return payload;
    }
}

