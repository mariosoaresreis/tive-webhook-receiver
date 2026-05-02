CREATE TABLE IF NOT EXISTS tive_alerts (
    id                  VARCHAR(36) PRIMARY KEY,
    tracker_id          VARCHAR(255) NOT NULL,
    alert_id            VARCHAR(255),
    alert_date          VARCHAR(255),
    alert_name          VARCHAR(255),
    alert_type          VARCHAR(255),
    alert_type_id       INTEGER,
    alert_value         TEXT,
    alert_display_value TEXT,
    trigger_name        VARCHAR(255),
    trigger_value       VARCHAR(255),
    device_id           VARCHAR(255),
    shipment_id         VARCHAR(255),
    payload_json        TEXT NOT NULL,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tive_alerts_tracker_received
    ON tive_alerts (tracker_id, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_tive_alerts_alert_date
    ON tive_alerts (alert_date);

CREATE UNIQUE INDEX IF NOT EXISTS uq_tive_alerts_alert_id
    ON tive_alerts (tracker_id, alert_id)
    WHERE alert_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_tive_alerts_fallback
    ON tive_alerts (tracker_id, alert_type, alert_date)
    WHERE alert_id IS NULL;

