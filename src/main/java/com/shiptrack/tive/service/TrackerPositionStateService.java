package com.shiptrack.tive.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiptrack.tive.model.TiveWebhookPayload;
import com.shiptrack.tive.model.TrackerPositionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackerPositionStateService {

    private static final String KEY_PREFIX = "tive:tracker:position:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void upsertLatest(TiveWebhookPayload payload) {
        if (!isPayloadUsable(payload)) {
            return;
        }

        String trackerId = payload.getEntityName();
        long incomingTs = payload.getEntryTimeEpoch() != null ? payload.getEntryTimeEpoch() : System.currentTimeMillis();
        String key = KEY_PREFIX + trackerId;

        Optional<TrackerPositionState> current = findCurrent(trackerId);
        if (current.isPresent() && current.get().getEntryTimeEpoch() != null
                && current.get().getEntryTimeEpoch() > incomingTs) {
            log.debug("Ignoring stale position event. tracker={} incomingTs={} currentTs={}",
                    trackerId, incomingTs, current.get().getEntryTimeEpoch());
            return;
        }

        TrackerPositionState next = TrackerPositionState.builder()
                .trackerId(trackerId)
                .latitude(payload.getLocation().getLatitude())
                .longitude(payload.getLocation().getLongitude())
                .entryTimeEpoch(incomingTs)
                .entryTimeUtc(payload.getEntryTimeUtc())
                .projectedAtEpoch(System.currentTimeMillis())
                .build();

        redisTemplate.opsForValue().set(key, writeJson(next));
    }

    public Optional<TrackerPositionState> findCurrent(String trackerId) {
        if (!StringUtils.hasText(trackerId)) {
            return Optional.empty();
        }

        String raw = redisTemplate.opsForValue().get(KEY_PREFIX + trackerId);
        if (raw == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(raw, TrackerPositionState.class));
        } catch (JsonProcessingException ex) {
            log.error("Invalid position state payload in Redis. tracker={}", trackerId, ex);
            return Optional.empty();
        }
    }

    private boolean isPayloadUsable(TiveWebhookPayload payload) {
        if (payload == null || !StringUtils.hasText(payload.getEntityName()) || payload.getLocation() == null) {
            log.debug("Ignoring position projection due to missing tracker or location");
            return false;
        }

        if (payload.getLocation().getLatitude() == null || payload.getLocation().getLongitude() == null) {
            log.debug("Ignoring position projection due to missing coordinates. tracker={}", payload.getEntityName());
            return false;
        }

        return true;
    }

    private String writeJson(TrackerPositionState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize tracker position state", ex);
        }
    }
}
