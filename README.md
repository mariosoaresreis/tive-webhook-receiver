# tive-webhook-receiver

Spring Boot service for real-time ingestion of events from [Tive](https://www.tive.com) 5G trackers.

## Architecture

```
Tive Platform
     |
     |  HTTP POST (webhook)
     |  X-Tive-Client-Id / X-Tive-Client-Secret
     v
WebhookAuthenticationFilter      <- validates credentials, injects correlationId into MDC
     |
     v
TiveWebhookController            <- accepts payload, responds 200 IMMEDIATELY
     |
IdempotencyService (Redis)       <- drops duplicates (Tive re-delivers on failures)
     |
TiveEventPublisher               <- publishes to Kafka asynchronously
     |
     |- tive.positions  (GPS + sensors)  key = trackerId
     |- tive.alerts     (shock, temp...) key = trackerId
     \- tive.dlq        (failures)

tive.alerts (consumer group: tive-alert-persistence)
     |
     v
TiveAlertsPersistenceConsumer    <- persists alerts for querying/audit
     |
     v
PostgreSQL (Cloud SQL in GCP)

tive.positions (consumer group: tive-position-state)
     |
     v
TivePositionsProjectionConsumer  <- projects latest position per tracker
     |
     v
Redis latest state               <- key tive:tracker:position:{trackerId}
     |
     v
TrackerPositionController        <- GET /trackers/{id}/position
```

### Why respond 200 before Kafka confirms?

Tive has a short delivery timeout. If the endpoint takes too long, it re-delivers the webhook, causing a duplicate storm. The controller responds immediately; Kafka failures are handled in the asynchronous callback (sent to DLQ and recovered later by a reprocessor).

### Idempotency

Tive attempts to re-deliver failed webhooks. `IdempotencyService` uses `SET NX EX` in Redis (atomic operation) to ensure each event is processed exactly once, even when duplicates are received.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/webhooks/tive/positions` | GPS and sensor data |
| POST | `/webhooks/tive/alerts` | Alerts (shock, temperature, geofence) |
| GET  | `/webhooks/tive/health` | Health check for Tive to validate the endpoint |
| GET  | `/trackers/{trackerId}/position` | Returns the latest projected position from Redis |
| GET  | `/actuator/prometheus` | Metrics for Prometheus/Grafana |

## Authentication

Required headers in all requests:
```
X-Tive-Client-Id: <your-client-id>
X-Tive-Client-Secret: <your-client-secret>
```

## Quick Start

```bash
# Start Kafka, Redis, and the application
docker-compose up -d

# Simulate a position webhook
curl -X POST http://localhost:8080/webhooks/tive/positions \
  -H "Content-Type: application/json" \
  -H "X-Tive-Client-Id: my-client-id" \
  -H "X-Tive-Client-Secret: my-client-secret" \
  -d '{
    "EntityName": "TRACKER-001",
    "EntryTimeEpoch": 1712345678000,
    "EntryTimeUtc": "2024-04-05T20:00:00Z",
    "Location": { "Latitude": -23.5505, "Longitude": -46.6333 },
    "Temperature": { "Celsius": 22.5 },
    "Battery": { "Percentage": 85.0, "IsCharging": false }
  }'

# Query current position for a tracker
curl http://localhost:8080/trackers/TRACKER-001/position

# Inspect messages in Kafka
xdg-open http://localhost:8090
```

## Configuration

| Environment variable | Default | Description |
|----------------------|---------|-------------|
| `KAFKA_BROKERS` | `localhost:9092` | Kafka bootstrap servers |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `TIVE_CLIENT_ID` | - | Client ID provided by Tive |
| `TIVE_CLIENT_SECRET` | - | Secret provided by Tive |
| `TIVE_POSITION_STATE_CONSUMER_GROUP` | `tive-position-state` | Kafka consumer group for latest-position projection |
| `TIVE_ALERT_PERSISTENCE_CONSUMER_GROUP` | `tive-alert-persistence` | Kafka consumer group used to persist alerts into PostgreSQL |
| `TIVE_ALERT_PERSISTENCE_ENABLED` | `true` | Enables/disables the PostgreSQL alert persistence flow |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `tive` | PostgreSQL database name |
| `DB_USER` | `tive` | PostgreSQL username |
| `DB_PASSWORD` | `tive` | PostgreSQL password |

## Deploy to Google Cloud

1. Create Artifact Registry repository and Cloud SQL PostgreSQL instance.
2. Store `DB_PASSWORD` and `TIVE_CLIENT_SECRET` in Secret Manager.
3. Update substitutions in `cloudbuild.yaml`.
4. Run Cloud Build to build, push, and deploy to Cloud Run.

```bash
gcloud builds submit --config cloudbuild.yaml
```

The Cloud Run deployment uses profile `gcp` (`application-gcp.yml`) and connects to Cloud SQL via the Cloud SQL JDBC Socket Factory.

## Available metrics

| Metric | Description |
|--------|-------------|
| `tive.webhook.received{type=position}` | Received positions |
| `tive.webhook.received{type=alert}` | Received alerts |
| `tive.webhook.duplicates` | Events dropped due to duplication |
| `tive.webhook.published{topic=...}` | Events published to Kafka |
| `tive.webhook.publish.failures` | Publish failures (sent to DLQ) |
| `tive.webhook.latency` | Webhook processing latency |

## Tests

```bash
mvn test
```

Tests use `@EmbeddedKafka` (no Docker required in CI).
