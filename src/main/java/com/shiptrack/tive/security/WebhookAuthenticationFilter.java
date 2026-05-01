package com.shiptrack.tive.security;

import com.shiptrack.tive.config.TiveWebhookProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that:
 * 1. Generates a unique correlationId per request and injects it into the MDC (appears in all logs)
 * 2. Validates the X-Tive-Client-Id and X-Tive-Client-Secret headers
 * 3. Rejects with 401 if authentication fails — without reaching the controller
 *
 * Applies only to the /webhooks/tive path
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_CLIENT_ID     = "X-Tive-Client-Id";
    private static final String HEADER_CLIENT_SECRET = "X-Tive-Client-Secret";
    private static final String WEBHOOK_PATH         = "/webhooks/tive";

    private final TiveWebhookProperties webhookProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Injects correlationId into MDC — appears in ALL logs of this thread
        String correlationId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("correlationId", correlationId);
        response.setHeader("X-Correlation-Id", correlationId);

        try {
            if (request.getRequestURI().startsWith(WEBHOOK_PATH)) {
                String clientId     = request.getHeader(HEADER_CLIENT_ID);
                String clientSecret = request.getHeader(HEADER_CLIENT_SECRET);

                if (!isAuthenticated(clientId, clientSecret)) {
                    log.warn("Webhook rejected: invalid credentials. clientId={}", clientId);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\"}");
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private boolean isAuthenticated(String clientId, String clientSecret) {
        return webhookProperties.getClientId().equals(clientId)
                && webhookProperties.getClientSecret().equals(clientSecret);
    }
}
