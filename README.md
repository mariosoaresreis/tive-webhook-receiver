# tive-webhook-receiver

Serviço Spring Boot para ingestão em tempo real de eventos dos trackers 5G [Tive](https://www.tive.com).

## Arquitetura

```
Tive Platform
     │
     │  HTTP POST (webhook)
     │  X-Tive-Client-Id / X-Tive-Client-Secret
     ▼
WebhookAuthenticationFilter      ← valida credenciais, injeta correlationId no MDC
     │
     ▼
TiveWebhookController            ← aceita o payload, responde 200 IMEDIATO
     │
IdempotencyService (Redis)       ← descarta duplicatas (Tive re-entrega em falhas)
     │
TiveEventPublisher               ← publica no Kafka de forma assíncrona
     │
     ├─ tive.positions  (GPS + sensores)   key = trackerId
     ├─ tive.alerts     (choque, temp...)  key = trackerId
     └─ tive.dlq        (falhas)
```

### Por que responder 200 antes do Kafka confirmar?

A Tive tem timeout curto nas entregas. Se o endpoint demorar, ela re-entrega o webhook — causando storm de duplicatas. O controller responde instantaneamente; falhas no Kafka são tratadas no callback assíncrono (vai para DLQ e um reprocessador recupera depois).

### Idempotência

A Tive tenta re-entregar webhooks que falham. O `IdempotencyService` usa `SET NX EX` no Redis (operação atômica) para garantir que cada evento seja processado exatamente uma vez, mesmo recebendo duplicatas.

## Endpoints

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/webhooks/tive/positions` | Dados de GPS e sensores |
| POST | `/webhooks/tive/alerts` | Alertas (choque, temperatura, geofence) |
| GET  | `/webhooks/tive/health` | Health check para a Tive validar o endpoint |
| GET  | `/actuator/prometheus` | Métricas para Prometheus/Grafana |

## Autenticação

Headers obrigatórios em todas as requisições:
```
X-Tive-Client-Id: <seu-client-id>
X-Tive-Client-Secret: <seu-client-secret>
```

## Quick Start

```bash
# Subir Kafka, Redis e a aplicação
docker-compose up -d

# Simular um webhook de posição
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

# Inspecionar mensagens no Kafka
open http://localhost:8090
```

## Configuração

| Variável de ambiente | Padrão | Descrição |
|---------------------|--------|-----------|
| `KAFKA_BROKERS` | `localhost:9092` | Bootstrap servers do Kafka |
| `REDIS_HOST` | `localhost` | Host do Redis |
| `REDIS_PORT` | `6379` | Porta do Redis |
| `TIVE_CLIENT_ID` | — | Client ID fornecido pela Tive |
| `TIVE_CLIENT_SECRET` | — | Secret fornecido pela Tive |

## Métricas disponíveis

| Métrica | Descrição |
|---------|-----------|
| `tive.webhook.received{type=position}` | Posições recebidas |
| `tive.webhook.received{type=alert}` | Alertas recebidos |
| `tive.webhook.duplicates` | Eventos descartados por duplicidade |
| `tive.webhook.published{topic=...}` | Eventos publicados no Kafka |
| `tive.webhook.publish.failures` | Falhas de publicação (foram para DLQ) |
| `tive.webhook.latency` | Latência de processamento do webhook |

## Testes

```bash
mvn test
```

Os testes usam `@EmbeddedKafka` (sem Docker necessário em CI).
