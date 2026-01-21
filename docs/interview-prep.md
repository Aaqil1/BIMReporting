# Interview Prep Cheat Sheet

## Design patterns (talk track)
- Strategy + Factory for report types -> easy extensibility, Open/Closed.
- Idempotent consumer flow -> safe retries; DB status gate.
- CircuitBreaker + Retry on Archive -> prevents cascading failures.
- CorrelationId logging -> end-to-end traceability.
- Cite code: Strategy selection in ReportStrategyFactory, idempotent guards in ReportRequestedListener, CB/Retry in ArchiveDbClient.

## API gateway / edge
- Gateway validates JWT (issuer/audience/exp/nbf/signature) and scopes (reports.write for POST, reports.read for GETs) before the service; service still validates JWT (defense in depth).
- Edge handles rate limiting, WAF, CORS, and injects X-Correlation-Id/traceparent; optional mTLS gateway->service.
- Edge observability: route latency, 4xx/5xx, rate-limit hits, auth failures, structured logs.

## Kafka focus (Q&A)
- Producers: API publishes ReportRequestedEvent with key=requestId to bim-report-requested.
- Consumers: group bim-report-workers, concurrency=1, 6 partitions -> scale pods to 6.
- Reliability: manual commit, retries with exponential backoff, DLQ, idempotency via DB status.
- Ordering: key-based; per-request ordering guaranteed within partition.
- Payload to quote: requestId/reportType/requestedBy/parametersJson/requestedAt JSON.
- Improvements: lag-based HPA, schema registry, DLQ replay tooling.

## Kubernetes focus
- Deployment + Service + Helm chart; env-driven config.
- Scaling: map pods to partitions; readiness/liveness + PDB; add HPA on lag/CPU.
- Config/secrets: move creds to Secrets; ConfigMaps for non-secret settings.

## Observability & ops
- Micrometer timers per report type; Actuator; JSON logs with correlationId.
- Suggest Prometheus/Grafana, log aggregation, tracing (OpenTelemetry), Kafka UI.

## Security
- OAuth2 resource server with scope-based method security.
- Recommend TLS to Kafka/MySQL, secret rotation, boundary checks on payload size/schema.

## Storytelling structure (step-by-step)
1) REST -> Kafka -> Consumer -> Strategy -> Archive -> DB status.
2) Safeguards: idempotency, retries/DLQ, circuit breaker, authz.
3) Ops maturity: containerization, K8s deployment, observability hooks.
4) Improvements: autoscaling, tracing, schema governance, replay tooling.

## Quick request/response examples
- POST /api/v1/reports/generate -> {"requestId":"<uuid>"} 202
- GET /api/v1/reports/{id}/status -> {"requestId":"<uuid>","status":"IN_PROGRESS","errorMessage":null}
- GET /api/v1/reports/{id} -> includes archiveRef, parametersJson, timestamps
