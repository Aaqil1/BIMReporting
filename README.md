# reports-ms

Event-driven Spring Boot 3 microservice for BIM / AIMS Reporting Platform.

## Features
- REST APIs to submit and query report generation requests (JWT scopes: `SCOPE_reports.write` for POST, `SCOPE_reports.read` for GETs).
- Kafka-based async processing; heavy work in consumer only.
- Report types via Strategy + Factory (PERFORMANCE, BENCHMARK_SUMMARY, DIVERSIFICATION_BAR, ASSET_ALLOCATION).
- Idempotent consumer flow, retries with DLQ.
- MySQL persistence of report lifecycle and archive references.
- Archive service integration with WebClient + Resilience4j (Retry + CircuitBreaker).
- Observability: MDC correlationId, JSON logging, Micrometer timers, Actuator.
- Packaging: Dockerfile, docker-compose (Kafka + MySQL), K8s manifests, Helm chart.
- Tests: JUnit 5, Mockito, Instancio.

## Prerequisites
- Java 17, Maven 3.9+
- Docker + Docker Compose
- Kafka and MySQL (or use provided compose)
- JWT issuer/keys for resource server

## Build
```bash
mvn clean package
```

## Run locally (default profile)
```bash
mvn spring-boot:run
# or
java -jar target/reports-ms-1.0.0.jar
```

## Run with Docker Compose (Kafka + MySQL)
```bash
cd docker
docker-compose up -d
cd ..
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

## Run with Docker Compose + Kong API Gateway (optional)
```bash
# build the app image
mvn clean package
docker build -t reports-ms:1.0.0 -f docker/Dockerfile .
cd docker
docker-compose up -d
```
- Gateway proxy: http://localhost:8000
- Admin (lock down in real envs): http://localhost:8001
- Upstream app: http://localhost:8080

## REST APIs
- POST `/api/v1/reports/generate` -> 202 {requestId}
- GET `/api/v1/reports/{requestId}/status`
- GET `/api/v1/reports/{requestId}`
Requires JWT:
- POST scope: `SCOPE_reports.write`
- GET scopes: `SCOPE_reports.read`

## Kafka
- Topic: `bim-report-requested` (partitions: 6)
- DLQ: `bim-report-requested-dlq`
- Consumer group: `bim-report-workers`, concurrency=1 per pod
- Producer key = requestId

## Database (MySQL)
Table `report_request`:
- request_id (PK), report_type, status, requested_by, parameters_json, archive_ref, error_message, created_at, updated_at
Indexes on status and created_at.

## Archive Service
- POST `/api/v1/archive/reports` via WebClient
- Resilience4j CircuitBreaker + Retry
- Returns `archiveRef` persisted on completion

## Observability
- JSON Logback with MDC `correlationId`
- Micrometer timers per report type
- Actuator: health, info, prometheus

## Security
- OAuth2 Resource Server (JWT)
- Method security with scopes

## Tests
```bash
mvn test
```

## Docker image
```bash
mvn clean package
docker build -t reports-ms:1.0.0 -f docker/Dockerfile .
docker run -p 8080:8080 reports-ms:1.0.0
```

## Kubernetes
```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

## API Gateway (Kong declarative)
- Declarative config: `gateway/kong.yml`
- Compose mounts it for local gateway usage.

## Helm
```bash
cd helm
helm install reports-ms . \
  --set image.repository=reports-ms \
  --set image.tag=1.0.0
```

## Push to GitHub
```bash
git init
git remote add origin https://github.com/Aaqil1/BIMReporting.git
git add .
git commit -m "Add reports-ms service with README"
git branch -M main
git push -u origin main
```
