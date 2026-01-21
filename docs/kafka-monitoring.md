# Kafka Monitoring & Tooling

## Current state
- No UI tool checked in for Kafka offsets/lag/DLQ inspection.

## Recommended stack (step-by-step)
1) Add a lightweight UI (AKHQ or Kowl) via docker-compose/K8s chart to browse topics, partitions, lag, DLQ messages.
2) Enable Micrometer Kafka consumer metrics and scrape with Prometheus; build Grafana dashboards for lag, throughput, error rate, and retry/DLQ counts.
3) Alert on sustained lag, DLQ volume, and consumer restarts; tie alerts to on-call runbooks.
4) Use structured logging with `correlationId` to trace messages end-to-end; include partition/offset in logs for DLQ triage.
5) For reprocessing: document a DLQ replay script (consume, fix payload, republish to main topic with same key).

## Interview talking points
- “We monitor lag per partition, DLQ rates, and consumer errors; AKHQ/Kowl for inspection, Prometheus/Grafana for SLOs.”
- “Reprocessing is controlled: pull from DLQ, validate, and re-publish to the primary topic to maintain ordering.”
- “CorrelationId ties API calls to Kafka events and archive writes for root-cause analysis.”
