# Architecture Overview (BIM Reporting)

## Components
- API: `ReportsController` exposes POST `/generate`, GET `/status`, GET `/report`.
- Service: `ReportService` persists request, emits Kafka event, and serves status/details queries.
- Domain/Persistence: `ReportRequest` JPA entity + `ReportRequestRepository`.
- Kafka: `ReportEventPublisher` (producer) + `ReportRequestedListener` (consumer).
- Processing: `ReportStrategyFactory` + `ReportGenerationStrategy` implementations.
- Integration: `ArchiveDbClient` (WebClient + Resilience4j) for archive handoff.
- Config/observability: Security (JWT scopes), MDC filter, Micrometer timers, Logback JSON, Actuator.

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
