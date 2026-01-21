# Kubernetes & Helm (Deep Dive + Trainer Guide)

## Core basics (quick refresher)
- Control plane: API Server (front door), etcd (state), Scheduler (places pods), Controller Manager (reconcilers), Admission (mutating/validating webhooks).
- Workloads: Pod, Deployment (stateless rolling), StatefulSet (stable IDs), DaemonSet (one-per-node), Job/CronJob (batch).
- Services: ClusterIP, NodePort, LoadBalancer, Headless (service discovery).
- Config: ConfigMap (non-secret), Secret (confidential), Downward API, CSI volumes/PV/PVC, emptyDir for ephemeral data.
- Scheduling: labels/selectors, taints/tolerations, affinity/anti-affinity, topology spread, priority classes.
- Security: RBAC, ServiceAccounts, PodSecurity (Baseline/Restricted), NetworkPolicy, securityContext (runAsNonRoot, drop caps), mTLS to backends where required.

## Baseline (repo)
- Deployment: replicas=2, image `reports-ms:1.0.0`, env for DB/Kafka/archive, profile `local`.
- Service: ClusterIP on port 80 -> 8080.
- Helm chart: values for image, replicaCount, env overrides.

## Deploy commands (hands-on)
- Raw manifests:
  ```bash
  kubectl apply -f k8s/deployment.yaml
  kubectl apply -f k8s/service.yaml
  ```
- Helm with overrides:
  ```bash
  helm upgrade --install reports-ms ./helm \
    --set image.repository=reports-ms \
    --set image.tag=1.0.0 \
    --set replicaCount=2 \
    --set env.SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    --set env.SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/reports?useSSL=false
  ```

## Probes & lifecycle (high-confidence rollouts)
```yaml
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 3
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          periodSeconds: 20
          timeoutSeconds: 3
          failureThreshold: 3
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh","-c","sleep 10"] # allow Kafka consumer to drain before SIGTERM
```
- Ensure `terminationGracePeriodSeconds` > preStop sleep + max in-flight work time.

## Resources & JVM sizing
```yaml
        resources:
          requests:
            cpu: "250m"
            memory: "512Mi"
          limits:
            cpu: "1000m"
            memory: "1Gi"
        env:
          - name: JAVA_TOOL_OPTIONS
            value: "-XX:InitialRAMPercentage=50 -XX:MaxRAMPercentage=75"
```

## Scaling & Kafka alignment
- Consumer group: `bim-report-workers`, concurrency=1 -> each pod owns whole partitions.
- With 6 partitions, set replicas=6 for max parallelism; rebalancing redistributes partitions on scale/rollout.
- If you increase partitions, scale pods accordingly; keep key-based routing (requestId) to preserve ordering.

## HPA examples
- CPU-based:
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: reports-ms
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: reports-ms
  minReplicas: 2
  maxReplicas: 6
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```
- Lag-based (custom metric placeholder): scrape `kafka_consumer_records_lag_max` to Prometheus Adapter and target it:
```yaml
    - type: Pods
      pods:
        metric:
          name: kafka_consumer_records_lag_max
        target:
          type: AverageValue
          averageValue: "100"
```

## Pod disruption & placement
- PodDisruptionBudget:
```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: reports-ms-pdb
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: reports-ms
```
- Anti-affinity / spread:
```yaml
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchExpressions:
                  - key: app
                    operator: In
                    values: ["reports-ms"]
              topologyKey: "kubernetes.io/hostname"
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: "topology.kubernetes.io/zone"
          whenUnsatisfiable: DoNotSchedule
          labelSelector:
            matchLabels:
              app: reports-ms
```

## Config & secrets
- ConfigMap for non-secrets:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: reports-ms-config
data:
  SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
  ARCHIVE_DB_BASE_URL: http://archive-db:8080
```
- Secret for credentials (example; use External Secrets/Sealed Secrets in real envs):
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: reports-ms-secrets
type: Opaque
stringData:
  SPRING_DATASOURCE_USERNAME: reports_user
  SPRING_DATASOURCE_PASSWORD: reports_pass
```
- Mount/inject:
```yaml
        envFrom:
          - configMapRef:
              name: reports-ms-config
          - secretRef:
              name: reports-ms-secrets
```
- Use `valueFrom.secretKeyRef` for single keys when you need tighter scope.

## Networking & ingress
- Current chart exposes ClusterIP; pair with ingress/controller or API gateway (Kong) for TLS, auth, and rate limits.
- ServiceAccount + RBAC if pods need Kubernetes API access (not required here).
- NetworkPolicy example (allow only gateway + DNS):
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: reports-allow-gateway
spec:
  podSelector:
    matchLabels:
      app: reports-ms
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: kong
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: kube-system
      ports:
        - port: 8080
  policyTypes: ["Ingress"]
```

## Observability on K8s
- Expose `/actuator/prometheus`; add `ServiceMonitor` for Prometheus Operator.
- Log shipping: stdout -> sidecar/DaemonSet (Fluent Bit/Vector) to ELK/Cloud logging.
- Tracing: enable OpenTelemetry agent; propagate `traceparent` from gateway.

## Rollouts and safety
- RollingUpdate is default; for safer deploys consider canary/blue-green via Argo Rollouts or Helm hooks.
- Set `maxUnavailable=0`, `maxSurge=1` if you need zero-downtime plus capacity headroom.
- Set `terminationGracePeriodSeconds` >= Kafka processing window; use preStop sleep to finish in-flight.
- Use `revisionHistoryLimit` to control stored ReplicaSets; `progressDeadlineSeconds` to detect stalled rollouts.

## Init/wait strategies
- InitContainer to wait for Kafka/MySQL DNS/connectivity in lower envs (do not block indefinitely in prod).
- Use `startupProbe` if JVM needs warm-up (e.g., large heaps).

## Security hardening
- Run as non-root, read-only root FS, drop capabilities:
```yaml
        securityContext:
          runAsNonRoot: true
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          capabilities:
            drop: ["ALL"]
```
- TLS to Kafka/MySQL via mounted certs if required; configure truststores via env/volume.
- Namespace isolation and PodSecurity admission (Baseline/Restricted) for multi-tenant clusters.

## Debugging and ops (trainer tips)
- Inspect rollout: `kubectl rollout status deploy/reports-ms`; undo: `kubectl rollout undo deploy/reports-ms`.
- Scheduling issues: `kubectl describe pod <pod>` (events), check taints/tolerations.
- Port-forward: `kubectl port-forward deploy/reports-ms 8080:8080` for quick API checks.
- Exec: `kubectl exec -it deploy/reports-ms -- sh` (if shell exists).
- Logs: `kubectl logs deploy/reports-ms --since=10m`; add `-p` for previous container.
- Topline: `kubectl get pods -o wide`, `kubectl top pods` (requires metrics-server).
- Stuck rollout: check readinessProbe failures, image pulls, failing initContainers.

## What to mention in interviews
- Mapping: replicas <-> Kafka partitions; readiness/liveness; preStop drain for Kafka.
- Resilience: PDB, anti-affinity, HPA on lag, timeouts around downstreams.
- Config/secrets separation and rotation; observability via Prometheus + logs + traces.
- Rollout safety and zero-downtime strategies; resource tuning tied to JVM heap sizing.
