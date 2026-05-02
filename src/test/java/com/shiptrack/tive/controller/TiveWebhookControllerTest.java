package com.shiptrack.tive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiptrack.tive.model.TiveWebhookPayload;
import com.shiptrack.tive.service.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"tive.positions", "tive.alerts", "tive.dlq"})
@TestPropertySource(properties = {
    "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
    "tive.alert-persistence.enabled=false",
    "tive.webhook.client-id=test-client",
    "tive.webhook.client-secret=test-secret"
})
class TiveWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setup() {
        when(idempotencyService.isNew(anyString())).thenReturn(true);
    }

    @Test
    void shouldReturn200ForValidPositionWebhook() throws Exception {
        TiveWebhookPayload payload = buildPayload("TRACKER-001", -23.5505, -46.6333);

        mockMvc.perform(post("/webhooks/tive/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tive-Client-Id", "test-client")
                .header("X-Tive-Client-Secret", "test-secret")
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WhenCredentialsMissing() throws Exception {
        TiveWebhookPayload payload = buildPayload("TRACKER-001", -23.5505, -46.6333);

        mockMvc.perform(post("/webhooks/tive/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200EvenForDuplicateEvent() throws Exception {
        TiveWebhookPayload payload = buildPayload("TRACKER-002", -22.9068, -43.1729);
        String body = objectMapper.writeValueAsString(payload);
        when(idempotencyService.isNew(anyString())).thenReturn(true, false);

        // First request
        mockMvc.perform(post("/webhooks/tive/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tive-Client-Id", "test-client")
                .header("X-Tive-Client-Secret", "test-secret")
                .content(body))
                .andExpect(status().isOk());

        // Duplicate re-delivery — should still return 200
        mockMvc.perform(post("/webhooks/tive/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tive-Client-Id", "test-client")
                .header("X-Tive-Client-Secret", "test-secret")
                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/webhooks/tive/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tive-Client-Id", "test-client")
                .header("X-Tive-Client-Secret", "test-secret")
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    // ─── Builder helper ───────────────────────────────────────────────────────

    private TiveWebhookPayload buildPayload(String trackerId, double lat, double lon) {
        TiveWebhookPayload payload = new TiveWebhookPayload();
        payload.setEntityName(trackerId);
        payload.setEntryTimeEpoch(System.currentTimeMillis());
        payload.setEntryTimeUtc("2024-01-15T10:30:00Z");

        TiveWebhookPayload.Location location = new TiveWebhookPayload.Location();
        location.setLatitude(lat);
        location.setLongitude(lon);
        payload.setLocation(location);

        TiveWebhookPayload.Temperature temp = new TiveWebhookPayload.Temperature();
        temp.setCelsius(22.5);
        payload.setTemperature(temp);

        TiveWebhookPayload.Battery battery = new TiveWebhookPayload.Battery();
        battery.setPercentage(85.0);
        battery.setIsCharging(false);
        payload.setBattery(battery);

        return payload;
    }
}
