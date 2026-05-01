package com.shiptrack.tive.service;

import com.shiptrack.tive.config.TiveWebhookProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Ensures idempotency via Redis:
 * - If the same event has already been processed (same key = trackerId + timestamp),
 *   returns false and the controller silently discards it.
 * - Tive may re-deliver the same webhook on timeout (automatic retries),
 *   so this service prevents processing duplicates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String KEY_PREFIX = "tive:idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final TiveWebhookProperties properties;

    /**
     * Attempts to register the event as processed.
     *
     * @param idempotencyKey unique event key (e.g.: trackerId + "::" + entryTimeEpoch)
     * @return true if it is new (should be processed), false if it has been seen before
     */
    public boolean isNew(String idempotencyKey) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        Duration ttl = Duration.ofSeconds(properties.getIdempotencyTtlSeconds());

        // setIfAbsent = SET NX EX — atomic in Redis
        Boolean inserted = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", ttl);

        boolean isNew = Boolean.TRUE.equals(inserted);

        if (!isNew) {
            log.debug("Duplicate event ignored. key={}", idempotencyKey);
        }

        return isNew;
    }
}
