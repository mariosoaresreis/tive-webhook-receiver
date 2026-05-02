package com.shiptrack.tive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiptrack.tive.model.TiveAlertPayload;
import com.shiptrack.tive.repository.AlertRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlertPersistenceServiceTest {

    @Mock
    private AlertRecordRepository alertRecordRepository;

    private AlertPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new AlertPersistenceService(alertRecordRepository, new ObjectMapper());
    }

    @Test
    void shouldMapAndPersistAlert() {
        TiveAlertPayload payload = buildPayload();

        service.persistAlert(payload);

        ArgumentCaptor<com.shiptrack.tive.model.AlertRecord> captor =
                ArgumentCaptor.forClass(com.shiptrack.tive.model.AlertRecord.class);
        verify(alertRecordRepository).save(captor.capture());

        com.shiptrack.tive.model.AlertRecord persisted = captor.getValue();
        assertThat(persisted.getTrackerId()).isEqualTo("TRACKER-01");
        assertThat(persisted.getAlertId()).isEqualTo("alert-123");
        assertThat(persisted.getPayloadJson()).contains("TRACKER-01");
        assertThat(persisted.getReceivedAt()).isNotNull();
    }

    @Test
    void shouldIgnoreDuplicateConstraintViolations() {
        doThrow(new DataIntegrityViolationException("duplicate")).when(alertRecordRepository).save(any());

        assertDoesNotThrow(() -> service.persistAlert(buildPayload()));
    }

    private TiveAlertPayload buildPayload() {
        TiveAlertPayload payload = new TiveAlertPayload();
        payload.setEntityName("TRACKER-01");
        payload.setAlertDate("2026-05-02T12:00:00Z");
        payload.setAlertType("TEMPERATURE");
        payload.setAlertDisplayValue("8.2 C");

        TiveAlertPayload.AlertDetail detail = new TiveAlertPayload.AlertDetail();
        detail.setAlertId("alert-123");
        detail.setDeviceId("device-1");
        payload.setAlert(detail);

        return payload;
    }
}

