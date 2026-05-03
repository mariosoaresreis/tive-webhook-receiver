package com.shiptrack.tive.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiptrack.tive.config.TiveRecoveryProperties;
import com.shiptrack.tive.model.TiveAlertPayload;
import com.shiptrack.tive.model.TiveWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TiveRecoveryClient {

    private static final List<String> DEFAULT_ITEMS_FIELDS = List.of("data", "items", "results", "records", "events");

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final TiveRecoveryProperties properties;

    public List<TiveWebhookPayload> fetchPositions(Instant fromInclusive, Instant toExclusive) {
        return fetchAllPages(properties.getPositions(), TiveWebhookPayload.class, fromInclusive, toExclusive, "positions");
    }

    public List<TiveAlertPayload> fetchAlerts(Instant fromInclusive, Instant toExclusive) {
        return fetchAllPages(properties.getAlerts(), TiveAlertPayload.class, fromInclusive, toExclusive, "alerts");
    }

    private <T> List<T> fetchAllPages(
            TiveRecoveryProperties.Endpoint endpoint,
            Class<T> payloadType,
            Instant fromInclusive,
            Instant toExclusive,
            String streamName
    ) {
        if (!endpoint.hasPath()) {
            log.debug("Skipping Tive recovery fetch because the endpoint is not configured. stream={}", streamName);
            return List.of();
        }

        List<T> recovered = new ArrayList<>();
        boolean paginate = StringUtils.hasText(properties.getPageParameter());
        int page = 1;

        while (true) {
            JsonNode body = restClientBuilder.build()
                    .get()
                    .uri(buildUri(endpoint.getPath(), fromInclusive, toExclusive, page, paginate))
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(this::applyHeaders)
                    .retrieve()
                    .body(JsonNode.class);

            List<T> pageItems = convertPayloads(body, endpoint.getResponseItemsField(), payloadType);
            recovered.addAll(pageItems);

            if (!paginate || pageItems.size() < properties.getPageSize()) {
                break;
            }

            page++;
            if (page > properties.getMaxPages()) {
                log.warn("Stopping Tive recovery pagination after reaching the configured limit. stream={} maxPages={}",
                        streamName, properties.getMaxPages());
                break;
            }
        }

        return recovered;
    }

    private URI buildUri(String endpointPath, Instant fromInclusive, Instant toExclusive, int page, boolean paginate) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(resolveUrl(endpointPath));

        if (StringUtils.hasText(properties.getFromParameter())) {
            builder.queryParam(properties.getFromParameter(), DateTimeFormatter.ISO_INSTANT.format(fromInclusive));
        }
        if (StringUtils.hasText(properties.getToParameter())) {
            builder.queryParam(properties.getToParameter(), DateTimeFormatter.ISO_INSTANT.format(toExclusive));
        }
        if (paginate && StringUtils.hasText(properties.getPageParameter())) {
            builder.queryParam(properties.getPageParameter(), page);
        }
        if (StringUtils.hasText(properties.getPageSizeParameter())) {
            builder.queryParam(properties.getPageSizeParameter(), properties.getPageSize());
        }

        return builder.build(true).toUri();
    }

    private void applyHeaders(HttpHeaders headers) {
        if (StringUtils.hasText(properties.getClientId())) {
            headers.add("X-Tive-Client-Id", properties.getClientId());
        }
        if (StringUtils.hasText(properties.getClientSecret())) {
            headers.add("X-Tive-Client-Secret", properties.getClientSecret());
        }
    }

    private String resolveUrl(String endpointPath) {
        if (endpointPath.startsWith("http://") || endpointPath.startsWith("https://")) {
            return endpointPath;
        }

        String baseUrl = properties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return endpointPath;
        }

        if (baseUrl.endsWith("/") && endpointPath.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + endpointPath;
        }
        if (!baseUrl.endsWith("/") && !endpointPath.startsWith("/")) {
            return baseUrl + "/" + endpointPath;
        }
        return baseUrl + endpointPath;
    }

    private <T> List<T> convertPayloads(JsonNode responseBody, String configuredItemsField, Class<T> payloadType) {
        JsonNode itemsNode = findItemsNode(responseBody, configuredItemsField);
        if (itemsNode == null || itemsNode.isNull()) {
            return List.of();
        }

        if (itemsNode.isArray()) {
            List<T> items = new ArrayList<>();
            for (JsonNode node : itemsNode) {
                items.add(objectMapper.convertValue(node, payloadType));
            }
            return items;
        }

        if (itemsNode.isObject()) {
            return List.of(objectMapper.convertValue(itemsNode, payloadType));
        }

        log.warn("Ignoring unsupported Tive recovery response body. nodeType={}", itemsNode.getNodeType());
        return List.of();
    }

    private JsonNode findItemsNode(JsonNode responseBody, String configuredItemsField) {
        if (responseBody == null || responseBody.isNull()) {
            return null;
        }

        if (responseBody.isArray()) {
            return responseBody;
        }

        if (!responseBody.isObject()) {
            return null;
        }

        if (StringUtils.hasText(configuredItemsField) && responseBody.has(configuredItemsField)) {
            return responseBody.get(configuredItemsField);
        }

        for (String field : DEFAULT_ITEMS_FIELDS) {
            JsonNode candidate = responseBody.get(field);
            if (candidate != null && (candidate.isArray() || candidate.isObject())) {
                return candidate;
            }
        }

        if (looksLikePayload(responseBody)) {
            return responseBody;
        }

        Iterator<Map.Entry<String, JsonNode>> iterator = responseBody.fields();
        while (iterator.hasNext()) {
            JsonNode candidate = iterator.next().getValue();
            if (candidate != null && (candidate.isArray() || looksLikePayload(candidate))) {
                return candidate;
            }
        }

        return null;
    }

    private boolean looksLikePayload(JsonNode node) {
        return node != null && node.isObject() && node.has("EntityName");
    }
}


