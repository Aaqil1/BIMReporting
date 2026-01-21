# Interview Prep Cheat Sheet

## Design patterns (talk track)
- Strategy + Factory for report types → easy extensibility, Open/Closed.
- Idempotent consumer flow → safe retries; DB status gate.
- CircuitBreaker + Retry on Archive → prevents cascading failures.
- CorrelationId logging → end-to-end traceability.

## Kafka focus (Q&A)
- Producers: API publishes `ReportRequestedEvent` with key=requestId to `bim-report-requested`.
- Consumers: group `bim-report-workers`, concurrency=1, 6 partitions → scale pods to 6.
- Reliability: manual commit, retries with exponential backoff, DLQ, idempotency via DB status.
- Ordering: key-based; per-request ordering guaranteed within partition.
- Improvements to mention: lag-based HPA, schema registry, DLQ replay tooling.

## Kubernetes focus
- Deployment + Service + Helm chart; env-driven config.
- Scaling: map pods to partitions; use readiness/liveness + PDB; add HPA on lag/CPU.
- Config/secrets: move creds to Secrets; ConfigMaps for non-secret settings.

## Observability & ops
- Micrometer timers per report type; Actuator; JSON logs with correlationId.
- Suggest Prometheus/Grafana, log aggregation, and tracing (OpenTelemetry) as next steps.
- Monitoring gaps: add Kafka UI + consumer lag dashboards.

## Security
- OAuth2 resource server with scope-based method security.
- Recommend TLS to Kafka/MySQL, secret rotation, and boundary checks on payload size/schema.

## Storytelling structure (step-by-step)
1) Paint the flow: REST → Kafka → Consumer → Strategy → Archive → DB status.
2) Explain safeguards: idempotency, retries/DLQ, circuit breaker, authz.
3) Show operations maturity: containerization, K8s deployment, observability hooks.
4) Close with improvements: autoscaling, tracing, schema governance, replay tooling.
