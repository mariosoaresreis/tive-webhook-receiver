package com.shiptrack.tive.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TiveRecoveryCursorService {

    private static final String CURSOR_KEY_PREFIX = "tive:recovery:cursor:";
    private static final String RUN_LOCK_KEY = "tive:recovery:lock";

    private final StringRedisTemplate redisTemplate;

    public Optional<String> acquireRunLock(Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(RUN_LOCK_KEY, token, ttl);
        return Boolean.TRUE.equals(locked) ? Optional.of(token) : Optional.empty();
    }

    public void releaseRunLock(String token) {
        String currentToken = redisTemplate.opsForValue().get(RUN_LOCK_KEY);
        if (token != null && token.equals(currentToken)) {
            redisTemplate.delete(RUN_LOCK_KEY);
        }
    }

    public Optional<Instant> readCursor(RecoveryStream stream) {
        String raw = redisTemplate.opsForValue().get(cursorKey(stream));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Instant.ofEpochMilli(Long.parseLong(raw)));
        } catch (NumberFormatException ex) {
            log.warn("Ignoring invalid recovery cursor. stream={} value={}", stream, raw);
            return Optional.empty();
        }
    }

    public void updateCursor(RecoveryStream stream, Instant cursor) {
        redisTemplate.opsForValue().set(cursorKey(stream), String.valueOf(cursor.toEpochMilli()));
    }

    private String cursorKey(RecoveryStream stream) {
        return CURSOR_KEY_PREFIX + stream.name().toLowerCase();
    }

    public enum RecoveryStream {
        POSITIONS,
        ALERTS
    }
}

