# Kafka End-to-End (Interview Playbook)

## Topology
- Topic: `bim-report-requested` (6 partitions).
- DLQ: `bim-report-requested-dlq`.
- Producer: `ReportEventPublisher` publishes key = `requestId`.
- Consumer: `ReportRequestedListener`, group `bim-report-workers`, concurrency=1 (per pod).

## Producer payload (JSON on the wire)
```json
{
  "requestId": "7f6f13d7-...",
  "reportType": "PERFORMANCE",
  "requestedBy": "analyst@ej.com",
  "parametersJson": "{\"accountIds\":[\"A1\"],\"from\":\"2024-01-01\",\"to\":\"2024-12-31\"}",
  "requestedAt": "2024-01-01T12:00:01Z"
}
```

## Flow (step-by-step)
1) API stores SUBMITTED row, then publishes `ReportRequestedEvent` with key=requestId.
2) Kafka routes by key → preserves per-request ordering within a partition.
3) Consumer fetches, sets MDC correlationId, loads row, and short-circuits if COMPLETED/IN_PROGRESS (idempotent).
4) Marks IN_PROGRESS, runs strategy, archives payload, updates to COMPLETED with archiveRef.
5) Exceptions bubble to the container error handler; after retries, message goes to DLQ.

## Reliability knobs (from config)
- Manual commits (`ENABLE_AUTO_COMMIT=false`) + transactional DB update ensure side effects persist before ack.
- Backoff/retries: Exponential backoff (1s → 2s → 4s, 3 attempts) via `DefaultErrorHandler`.
- DLQ routing: `DeadLetterPublishingRecoverer` publishes failed records to `bim-report-requested-dlq` with same partition.
- Idempotency: DB status gate + requestId key; replay is safe.
- Ordering: per-key ordering held; scaling via partitions.

## Consumer commit pattern
- Spring commits the offset only after listener returns without exception.
- DB update happens in the same listener transaction, so status/archival writes are durable before offset commit.

## DLQ record shape
- Same key/value as original; destination `bim-report-requested-dlq`, partition mirrors source for replay ordering.
- Error handler adds headers with exception info.

## Scaling guidance
- 6 partitions → up to 6 active consumers per group. With concurrency=1 per pod, scale pods to 6 for max parallelism.
- If adding partitions later, ensure keys keep routing per request; rebalancing redistributes partitions automatically.

## Failure handling playbook (interview ready)
- Transient downstream (Archive) → retries + circuit breaker; if exhausted, event DLQs and row becomes FAILED.
- Poison message (bad payload) → direct DLQ after retries; investigate, fix data, and re-publish from DLQ.
- Consumer crash → Kafka redelivers uncommitted record; idempotent check prevents double-work.

## Operational tips
- Monitor lag per partition and rebalance by scaling pods.
- Use correlationId in logs to trace API → Kafka → Archive path.
- Prefer compact, schema-stable payloads; add headers for provenance when extending.
