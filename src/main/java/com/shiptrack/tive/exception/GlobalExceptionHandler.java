package com.shiptrack.tive.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global error handler:
 * - Invalid payload (malformed JSON) → 400
 * - Any other exception → 200 anyway
 *
 * Why return 200 on internal errors?
 * Tive re-delivers the webhook if it receives 4xx/5xx.
 * If the problem is on our side (parsing bug), re-delivering won't fix it
 * and will generate an event storm. Better to accept, log the error, and investigate.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.error("Invalid payload received from Tive: {}", ex.getMessage());
        // 400 for invalid payload — Tive should not re-deliver this
        return ResponseEntity.badRequest().body("{\"error\":\"invalid_payload\"}");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleUnexpected(Exception ex) {
        // Detailed log for the team, but 200 so Tive does not re-deliver
        log.error("Unexpected error while processing Tive webhook", ex);
        return ResponseEntity.ok().build();
    }
}
