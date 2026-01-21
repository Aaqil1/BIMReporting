# Design Patterns (Interview Walkthrough)

## Quick map to code
- Strategy + Factory for report generation: `ReportStrategyFactory` wires the right `ReportGenerationStrategy` (PERFORMANCE, BENCHMARK_SUMMARY, DIVERSIFICATION_BAR, ASSET_ALLOCATION) to keep the consumer thin.
- Resilience patterns: `ArchiveDbClient` uses Resilience4j `@Retry` + `@CircuitBreaker`.
- Idempotent consumer: `ReportRequestedListener` checks status before processing and exits on duplicates/completed.
- Correlation/structured logging: `MdcCorrelationFilter` seeds `correlationId` (also reused in Kafka consumer).
- Layered architecture: Controller → Service → Repository → Kafka/DB; DTOs isolate transport from domain.

## Code anchors (copy/paste ready)
- Strategy selection:
  ```java
  return switch (reportType) {
      case PERFORMANCE -> performanceReportStrategy;
      case BENCHMARK_SUMMARY -> benchmarkSummaryReportStrategy;
      case DIVERSIFICATION_BAR -> diversificationBarReportStrategy;
      case ASSET_ALLOCATION -> assetAllocationReportStrategy;
  };
  ```
- Idempotent consumer guard:
  ```java
  if (request.getStatus() == ReportStatus.COMPLETED) return;
  if (request.getStatus() == ReportStatus.IN_PROGRESS) return;
  request.setStatus(ReportStatus.IN_PROGRESS);
  ```
- Resilience on external call:
  ```java
  @CircuitBreaker(name = "archiveDb", fallbackMethod = "fallbackArchive")
  @Retry(name = "archiveDb")
  public String archiveReport(String requestId, String payloadJson) { ... }
  ```
- CorrelationId propagation:
  ```java
  // REST filter seeds MDC
  MDC.put("correlationId", hdrOrRandom);
  // Kafka listener re-hydrates
  MDC.put("correlationId", event.getRequestId());
  ```

## How to explain in interviews (step-by-step)
1) Start with the Strategy pattern: “Each report type has its own strategy implementing `ReportGenerationStrategy`, selected by `ReportStrategyFactory` so adding a new type doesn’t change orchestration.”
2) Mention why: “Keeps the Kafka consumer small, respects Open/Closed, and localizes metrics per report type.”
3) Call out idempotency: “Consumer reads from Kafka, checks DB status; skips IN_PROGRESS/COMPLETED to tolerate retries/re-delivery.”
4) Describe resilience: “Archive call is wrapped with CircuitBreaker + Retry; failures mark the record FAILED and surface to DLQ.”
5) Traceability: “Every request gets an MDC `correlationId` propagated through REST and Kafka logs.”
6) Layered boundaries: “DTOs for input/output, service for orchestration, repository for persistence, client for external Archive.”

## If challenged on trade-offs
- Strategy vs. if/else: Strategy avoids giant switch in consumer and isolates metrics/circuit-breaking per type.
- Idempotency vs. exactly-once: We use at-least-once with idempotent updates in DB; could move to transaction/outbox if business demands stricter semantics.
- CircuitBreaker placement: Around the Archive client to avoid cascading failure and to fail fast while retrying on transient faults.
