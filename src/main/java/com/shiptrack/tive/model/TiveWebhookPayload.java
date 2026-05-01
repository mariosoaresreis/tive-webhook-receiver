package com.shiptrack.tive.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

/**
 * Represents the standard payload sent by the Tive Platform in device data webhooks.
 * Unknown fields are ignored to maintain compatibility with future API versions.
 *
 * Reference: https://developers.tive.com/docs/data-structures
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TiveWebhookPayload {

    /** Tracker name/ID (e.g.: "VD0001") */
    @JsonProperty("EntityName")
    private String entityName;

    /** Event timestamp in milliseconds epoch */
    @JsonProperty("EntryTimeEpoch")
    private Long entryTimeEpoch;

    /** UTC timestamp of the event */
    @JsonProperty("EntryTimeUtc")
    private String entryTimeUtc;

    @JsonProperty("Location")
    private Location location;

    @JsonProperty("Temperature")
    private Temperature temperature;

    @JsonProperty("Humidity")
    private Humidity humidity;

    @JsonProperty("Accelerometer")
    private Accelerometer accelerometer;

    @JsonProperty("Shock")
    private Shock shock;

    @JsonProperty("Battery")
    private Battery battery;

    @JsonProperty("Cellular")
    private Cellular cellular;

    @JsonProperty("Shipment")
    private Shipment shipment;

    // ─── Nested models ────────────────────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        @JsonProperty("Latitude")
        private Double latitude;
        @JsonProperty("Longitude")
        private Double longitude;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Temperature {
        @JsonProperty("Celsius")
        private Double celsius;
        @JsonProperty("Fahrenheit")
        private Double fahrenheit;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Humidity {
        @JsonProperty("Percentage")
        private Double percentage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Accelerometer {
        @JsonProperty("G")
        private Double g;
        @JsonProperty("X")
        private Double x;
        @JsonProperty("Y")
        private Double y;
        @JsonProperty("Z")
        private Double z;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Shock {
        @JsonProperty("G")
        private Double g;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Battery {
        @JsonProperty("Percentage")
        private Double percentage;
        @JsonProperty("IsCharging")
        private Boolean isCharging;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cellular {
        @JsonProperty("SignalStrength")
        private String signalStrength;
        @JsonProperty("Dbm")
        private Integer dbm;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Shipment {
        @JsonProperty("Id")
        private String id;
        @JsonProperty("Description")
        private String description;
    }
}
