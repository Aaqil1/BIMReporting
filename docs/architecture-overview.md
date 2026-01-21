# Architecture Overview (BIM Reporting)

## Components
- API: `ReportsController` exposes POST `/generate`, GET `/status`, GET `/report`.
- Service: `ReportService` persists request, emits Kafka event, and serves status/details queries.
- Domain/Persistence: `ReportRequest` JPA entity + `ReportRequestRepository`.
- Kafka: `ReportEventPublisher` (producer) + `ReportRequestedListener` (consumer).
- Processing: `ReportStrategyFactory` + `ReportGenerationStrategy` implementations.
- Integration: `ArchiveDbClient` (WebClient + Resilience4j) for archive handoff.
- Config/observability: Security (JWT scopes), MDC filter, Micrometer timers, Logback JSON, Actuator.

## API contracts (practical)
- POST `/api/v1/reports/generate` (scope `reports.write`)
  - Request:
    ```json
    {
      "reportType": "PERFORMANCE",
      "requestedBy": "analyst@ej.com",
      "parameters": { "accountIds": ["A1","A2"], "from": "2024-01-01", "to": "2024-12-31" }
    }
    ```
  - Response 202:
    ```json
    { "requestId": "7f6f13d7-..." }
    ```
- GET `/api/v1/reports/{requestId}/status` (scope `reports.read`)
  - Response 200:
    ```json
    { "requestId": "7f6f13d7-...", "status": "IN_PROGRESS", "errorMessage": null }
    ```
- GET `/api/v1/reports/{requestId}` (scope `reports.read`)
  - Response 200:
    ```json
    {
      "requestId": "7f6f13d7-...",
      "reportType": "PERFORMANCE",
      "status": "COMPLETED",
      "requestedBy": "analyst@ej.com",
      "parametersJson": "{\"accountIds\":[\"A1\",\"A2\"],\"from\":\"2024-01-01\",\"to\":\"2024-12-31\"}",
      "archiveRef": "archive://bucket/key.pdf",
      "errorMessage": null,
      "createdAt": "2024-01-01T12:00:01Z",
      "updatedAt": "2024-01-01T12:00:20Z"
    }
    ```

## Example curl flow
```bash
TOKEN=... # JWT with scopes reports.write/reports.read
REQ=$(curl -s -X POST https://localhost:8080/api/v1/reports/generate \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"reportType":"PERFORMANCE","requestedBy":"analyst@ej.com","parameters":{"accountIds":["A1"],"from":"2024-01-01","to":"2024-12-31"}}')
REQ_ID=$(echo $REQ | jq -r .requestId)
curl -s -H "Authorization: Bearer $TOKEN" https://localhost:8080/api/v1/reports/$REQ_ID/status
curl -s -H "Authorization: Bearer $TOKEN" https://localhost:8080/api/v1/reports/$REQ_ID
```

## Flow (step-by-step)
1) Client calls POST `/api/v1/reports/generate` with report type + params (JWT scope `reports.write`).
2) `ReportService` generates `requestId`, stores `ReportRequest` as SUBMITTED, publishes `ReportRequestedEvent` keyed by `requestId`.
3) Kafka assigns the event to a partition (keyed by `requestId`), consumed by `ReportRequestedListener` (group `bim-report-workers`).
4) Consumer marks DB row IN_PROGRESS, selects strategy via `ReportStrategyFactory`, and generates the report.
5) Strategy calls `ArchiveDbClient` to persist the generated payload; returns `archiveRef`.
6) Consumer updates DB row to COMPLETED with `archiveRef`; on exceptions marks FAILED + errorMessage and lets the error handler route to DLQ after retries.
7) Client polls GET `/status` or `/report` (JWT scope `reports.read`) to view status/details.

## Responsibilities
- API/Service: validation, orchestration, idempotent request creation, event publishing.
- Kafka consumer: reliable processing, idempotency checks, error classification (retry/DLQ), and status transitions.
- Strategies: pure business logic + metrics per report type.
- Archive client: external I/O with resilience.
- DB: source of truth for lifecycle, parameters, archive reference, and errors.

## Operations + packaging
- Dockerfile packages the fat jar.
- docker-compose provides local Kafka/MySQL.
- K8s manifests and Helm chart deploy the service with env-driven config.

## Interview angle
- Emphasize separation of concerns + event-driven split: lightweight API vs. heavy async worker.
- Call out back-pressure: heavy work stays off the request thread; Kafka absorbs spikes.
- Mention observability + security baked in (Actuator, Micrometer, JSON logs, JWT scopes).
