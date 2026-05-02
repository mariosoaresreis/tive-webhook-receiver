package com.shiptrack.tive.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "tive_alerts", indexes = {
        @Index(name = "idx_tive_alerts_tracker_received", columnList = "tracker_id,received_at"),
        @Index(name = "idx_tive_alerts_alert_date", columnList = "alert_date")
})
public class AlertRecord {

    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Column(name = "tracker_id", nullable = false)
    private String trackerId;

    @Column(name = "alert_id")
    private String alertId;

    @Column(name = "alert_date")
    private String alertDate;

    @Column(name = "alert_name")
    private String alertName;

    @Column(name = "alert_type")
    private String alertType;

    @Column(name = "alert_type_id")
    private Integer alertTypeId;

    @Column(name = "alert_value")
    private String alertValue;

    @Column(name = "alert_display_value")
    private String alertDisplayValue;

    @Column(name = "trigger_name")
    private String triggerName;

    @Column(name = "trigger_value")
    private String triggerValue;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "shipment_id")
    private String shipmentId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}

