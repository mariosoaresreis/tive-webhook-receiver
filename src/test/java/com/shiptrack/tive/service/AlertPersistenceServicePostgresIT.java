package com.shiptrack.tive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiptrack.tive.model.AlertRecord;
import com.shiptrack.tive.model.TiveAlertPayload;
import com.shiptrack.tive.repository.AlertRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({AlertPersistenceService.class, AlertPersistenceServicePostgresIT.TestConfig.class})
class AlertPersistenceServicePostgresIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("tive")
            .withUsername("tive")
            .withPassword("tive");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AlertPersistenceService service;

    @Autowired
    private AlertRecordRepository repository;

    @Test
    void shouldPersistAlertInPostgres() {
        service.persistAlert(buildPayload("TRACKER-01", "alert-1", "TEMPERATURE", "2026-05-02T12:00:00Z"));

        List<AlertRecord> records = repository.findAll();
        assertThat(records).hasSize(1);

        AlertRecord record = records.get(0);
        assertThat(record.getTrackerId()).isEqualTo("TRACKER-01");
        assertThat(record.getAlertId()).isEqualTo("alert-1");
        assertThat(record.getAlertType()).isEqualTo("TEMPERATURE");
        assertThat(record.getPayloadJson()).contains("TRACKER-01");
    }

    @Test
    void shouldIgnoreDuplicatesUsingAlertIdConstraint() {
        TiveAlertPayload payload = buildPayload("TRACKER-02", "alert-dup", "SHOCK", "2026-05-02T13:00:00Z");

        service.persistAlert(payload);
        service.persistAlert(payload);

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void shouldIgnoreDuplicatesUsingFallbackConstraintWhenAlertIdMissing() {
        TiveAlertPayload payload = buildPayload("TRACKER-03", null, "GEOFENCE", "2026-05-02T14:00:00Z");

        service.persistAlert(payload);
        service.persistAlert(payload);

        assertThat(repository.count()).isEqualTo(1);
    }

    private TiveAlertPayload buildPayload(String trackerId, String alertId, String alertType, String alertDate) {
        TiveAlertPayload payload = new TiveAlertPayload();
        payload.setEntityName(trackerId);
        payload.setAlertDate(alertDate);
        payload.setAlertType(alertType);
        payload.setAlertDisplayValue("10 C");
        payload.setAlertValue("10");

        if (alertId != null) {
            TiveAlertPayload.AlertDetail detail = new TiveAlertPayload.AlertDetail();
            detail.setAlertId(alertId);
            detail.setDeviceId("device-1");
            payload.setAlert(detail);
        }

        return payload;
    }

    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}


