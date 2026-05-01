package com.shiptrack.tive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiptrack.tive.model.TiveWebhookPayload;
import com.shiptrack.tive.model.TrackerPositionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackerPositionStateServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TrackerPositionStateService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new TrackerPositionStateService(redisTemplate, objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldStoreLatestWhenNoExistingState() {
        when(valueOperations.get("tive:tracker:position:TRACKER-001")).thenReturn(null);

        service.upsertLatest(payload("TRACKER-001", 1000L, 10.0, 20.0));

        verify(valueOperations, times(1)).set(eq("tive:tracker:position:TRACKER-001"), anyString());
    }

    @Test
    void shouldIgnoreStaleEvent() throws Exception {
        TrackerPositionState current = TrackerPositionState.builder()
                .trackerId("TRACKER-001")
                .latitude(30.0)
                .longitude(40.0)
                .entryTimeEpoch(2000L)
                .entryTimeUtc("2024-01-01T00:00:02Z")
                .projectedAtEpoch(2100L)
                .build();
        when(valueOperations.get("tive:tracker:position:TRACKER-001"))
                .thenReturn(objectMapper.writeValueAsString(current));

        service.upsertLatest(payload("TRACKER-001", 1000L, 10.0, 20.0));

        verify(valueOperations, never()).set(eq("tive:tracker:position:TRACKER-001"), anyString());
    }

    @Test
    void shouldReturnCurrentStateWhenPresent() throws Exception {
        TrackerPositionState current = TrackerPositionState.builder()
                .trackerId("TRACKER-777")
                .latitude(-23.55)
                .longitude(-46.63)
                .entryTimeEpoch(1712345678000L)
                .entryTimeUtc("2024-04-05T20:00:00Z")
                .projectedAtEpoch(1712345679000L)
                .build();

        when(valueOperations.get("tive:tracker:position:TRACKER-777"))
                .thenReturn(objectMapper.writeValueAsString(current));

        Optional<TrackerPositionState> found = service.findCurrent("TRACKER-777");

        assertTrue(found.isPresent());
        assertEquals("TRACKER-777", found.get().getTrackerId());
        assertEquals(-23.55, found.get().getLatitude());
        assertEquals(-46.63, found.get().getLongitude());
    }

    private TiveWebhookPayload payload(String trackerId, Long ts, Double lat, Double lon) {
        TiveWebhookPayload payload = new TiveWebhookPayload();
        payload.setEntityName(trackerId);
        payload.setEntryTimeEpoch(ts);
        payload.setEntryTimeUtc("2024-01-01T00:00:00Z");

        TiveWebhookPayload.Location location = new TiveWebhookPayload.Location();
        location.setLatitude(lat);
        location.setLongitude(lon);
        payload.setLocation(location);

        return payload;
    }
}
