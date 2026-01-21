# Kubernetes & Helm (Interview Notes)

## What’s defined
- Deployment: replicas=2, image `reports-ms:1.0.0`, env for DB/Kafka/archive, profile `local`.
- Service: ClusterIP on port 80 → 8080.
- Helm chart: values for image, replicaCount, env overrides.

## How it runs (step-by-step)
1) Deploy via Helm: set image repo/tag and env (datasource, Kafka bootstrap, archive URL, profile).
2) Deployment rolls out pods; each pod runs consumer concurrency=1 (so partitions are spread across pods).
3) Service exposes HTTP to the cluster; Kafka/MySQL/Archive endpoints injected via env.
4) Actuator probes can be wired to readiness/liveness for rollout safety.

## Scaling & pod-to-partition mapping
- Consumer group `bim-report-workers` uses Kafka rebalancing; with 6 partitions and concurrency=1, scale to 6 pods for full parallelism.
- For more throughput, increase partitions first, then scale pods to match.

## Resiliency practices to mention
- RollingUpdate by default; add PodDisruptionBudget to avoid total drain during maintenance.
- Use readinessProbe on `/actuator/health/readiness` and livenessProbe on `/actuator/health/liveness`.
- Requests/limits to avoid CPU throttling; pin JVM heap via `JAVA_TOOL_OPTIONS` if needed.
- CircuitBreaker/Retry already protect Archive; add timeouts on DB/Kafka via env.

## Config & secrets
- Externalize creds via K8s Secrets (DB user/pass, JWT keys) and ConfigMaps for non-secrets (bootstrap servers, archive URL).
- Use Helm values to swap between envs; keep defaults minimal and override per environment.

## Interview refinements to propose
- Add HPA on CPU + Kafka lag (via custom metrics) to auto-scale workers.
- Add ServiceMonitor for Prometheus scraping; expose Micrometer Prometheus endpoint.
- Integrate PodAntiAffinity or topology spread to avoid single-node blast radius.
- Add InitContainers to wait for Kafka/MySQL readiness in lower envs.
