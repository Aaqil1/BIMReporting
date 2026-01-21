# Microservices & Communication

## Current services
- `reports-ms`: REST ingress + Kafka worker + Archive integration.
- Archive service (external dependency): receives payload and returns `archiveRef`.
- Supporting infra: Kafka, MySQL.

## Interaction patterns
- Sync REST (client → reports-ms) for submission/status/details.
- Async Kafka (reports-ms producer → reports-ms consumer) for heavy work.
- Sync HTTP (reports-ms → Archive) with CircuitBreaker/Retry.

## Lifecycle (step-by-step)
1) Client submits via REST; service persists request and emits Kafka event.
2) Consumer processes event, runs strategy, calls Archive, updates DB.
3) Client polls REST for status/details (or could subscribe to events if added).

## Extensibility ideas
- Add notifications service consuming COMPLETED events to push email/SMS.
- Add DLQ handler service to reprocess/repair failed records.
- Introduce API Gateway for auth/throttling and central observability.
- Use schema registry + versioned events for broader ecosystem integration.

## Interview framing
- Highlight segregation of concerns: API vs. worker vs. external Archive.
- Stress back-pressure via Kafka and idempotent processing.
- Mention evolution path: more services can subscribe to events without coupling to API.
