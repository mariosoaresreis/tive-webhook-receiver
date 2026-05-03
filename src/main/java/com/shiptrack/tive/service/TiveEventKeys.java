package com.shiptrack.tive.service;

import com.shiptrack.tive.model.TiveAlertPayload;
import com.shiptrack.tive.model.TiveWebhookPayload;
import org.springframework.util.StringUtils;

public final class TiveEventKeys {

    private static final String MISSING_TIMESTAMP = "missing-timestamp";

    private TiveEventKeys() {
    }

    public static String positionKey(TiveWebhookPayload payload) {
        String trackerId = payload != null ? payload.getEntityName() : null;
        if (payload == null) {
            return trackerId + "::" + MISSING_TIMESTAMP;
        }

        if (payload.getEntryTimeEpoch() != null) {
            return trackerId + "::" + payload.getEntryTimeEpoch();
        }

        if (StringUtils.hasText(payload.getEntryTimeUtc())) {
            return trackerId + "::" + payload.getEntryTimeUtc();
        }

        return trackerId + "::" + MISSING_TIMESTAMP;
    }

    public static String positionKey(String trackerId, Long entryTimeEpoch) {
        return trackerId + "::" + entryTimeEpoch;
    }

    public static String alertKey(TiveAlertPayload payload) {
        String alertId = null;
        if (payload != null && payload.getAlert() != null) {
            alertId = payload.getAlert().getAlertId();
        }

        if (!StringUtils.hasText(alertId)) {
            alertId = payload != null ? payload.getAlertDate() : null;
        }

        String trackerId = payload != null ? payload.getEntityName() : null;
        return trackerId + "::alert::" + alertId;
    }
}


