package com.shiptrack.tive.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiptrack.tive.model.AlertRecord;
import com.shiptrack.tive.model.TiveAlertPayload;
import com.shiptrack.tive.repository.AlertRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tive.alert-persistence.enabled", havingValue = "true", matchIfMissing = true)
public class AlertPersistenceService {

    private final AlertRecordRepository alertRecordRepository;
    private final ObjectMapper objectMapper;

    public void persistAlert(TiveAlertPayload payload) {
        AlertRecord record = new AlertRecord();
        record.setId(UUID.randomUUID().toString());
        record.setTrackerId(payload.getEntityName());
        record.setAlertId(extractAlertId(payload));
        record.setAlertDate(payload.getAlertDate());
        record.setAlertName(payload.getAlertName());
        record.setAlertType(payload.getAlertType());
        record.setAlertTypeId(payload.getAlertTypeId());
        record.setAlertValue(payload.getAlertValue());
        record.setAlertDisplayValue(payload.getAlertDisplayValue());
        record.setTriggerName(payload.getTriggerName());
        record.setTriggerValue(payload.getTriggerValue());
        record.setDeviceId(payload.getAlert() != null ? payload.getAlert().getDeviceId() : null);
        record.setShipmentId(payload.getAlert() != null ? payload.getAlert().getShipmentId() : null);
        record.setPayloadJson(toJson(payload));
        record.setReceivedAt(Instant.now());

        try {
            alertRecordRepository.save(record);
        } catch (DataIntegrityViolationException ex) {
            // Duplicate alerts can occur due to retries; persistence is idempotent at DB level.
            log.debug("Duplicate alert ignored by DB constraints. tracker={} alertId={}",
                    payload.getEntityName(), record.getAlertId());
        }
    }

    private String extractAlertId(TiveAlertPayload payload) {
        if (payload.getAlert() == null || payload.getAlert().getAlertId() == null || payload.getAlert().getAlertId().isBlank()) {
            return null;
        }
        return payload.getAlert().getAlertId();
    }

    private String toJson(TiveAlertPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize alert payload", ex);
        }
    }
}


