package com.shiptrack.tive.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents the alert payload sent by Tive (shock, temperature, geofence, etc).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TiveAlertPayload {

    @JsonProperty("EntityName")
    private String entityName;

    @JsonProperty("AlertDate")
    private String alertDate;

    @JsonProperty("AlertName")
    private String alertName;

    @JsonProperty("AlertType")
    private String alertType;

    @JsonProperty("AlertTypeId")
    private Integer alertTypeId;

    @JsonProperty("AlertValue")
    private String alertValue;

    @JsonProperty("AlertDisplayValue")
    private String alertDisplayValue;

    @JsonProperty("TriggerName")
    private String triggerName;

    @JsonProperty("TriggerValue")
    private String triggerValue;

    @JsonProperty("Alert")
    private AlertDetail alert;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlertDetail {
        @JsonProperty("DeviceId")
        private String deviceId;
        @JsonProperty("ShipmentId")
        private String shipmentId;
        @JsonProperty("AlertId")
        private String alertId;
    }
}
