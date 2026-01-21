# API Gateway & Edge Security (Industry Pattern)

## Role of the gateway
- Single entry point for clients; hides internal topology.
- Offload cross-cutting: authN/Z (JWT), rate limiting, WAF, TLS termination, request shaping, header enrichment (correlationId, client-id), response compression/caching (where safe).
- Observability fan-out: traces, metrics, structured logs at the edge.

## Recommended setup
- Gateway: Kong / NGINX / AWS API Gateway / Istio ingress.
- TLS termination at gateway; mTLS from gateway → services (optional but recommended).
- JWT validation at gateway (issuer/audience, exp/nbf, signature, key rotation via JWKS).
- Scope-based routing/authorization: enforce `reports.write` for POST, `reports.read` for GETs.
- Rate limiting & burst control per client/app; optional quota plans.
- Request/response size limits to avoid oversized payloads.
- Timeouts: short at edge (5–10s for sync APIs); retries disabled for POST (or idempotent only).
- CORS configured centrally for allowed origins.

## Security flow (JWT)
1) Client obtains access token (OAuth2 / OIDC).
2) Client calls gateway with `Authorization: Bearer <JWT>`.
3) Gateway validates token (sig + claims) and enforces scopes per route:
   - POST `/api/v1/reports/generate` → requires `reports.write`
   - GET `/api/v1/reports/{id}/status` → requires `reports.read`
   - GET `/api/v1/reports/{id}` → requires `reports.read`
4) Gateway injects vetted identity headers to upstream (`X-Principal`, `X-Scopes`) and strips user-supplied variants.
5) Service still runs resource-server validation (defense in depth).

## Observability at the edge
- Tracing: generate/propagate `traceparent`/`X-B3-*`; set `X-Correlation-Id` if absent.
- Metrics: per-route latency, status codes, rate-limit hits, auth failures.
- Logging: structured JSON with route, client-id, correlationId, status, latency, error cause.
- Dashboards: p99 latency per route, auth failures, 4xx/5xx rates, rate-limit rejects.

## Example Kong declarative snippet (illustrative)
```yaml
services:
  - name: reports-ms
    url: http://reports-ms:8080
    routes:
      - name: reports-generate
        paths: ["/api/v1/reports/generate"]
        methods: ["POST"]
plugins:
  - name: jwt
    config:
      key_claim_name: kid
      claims_to_verify: ["exp","nbf"]
      secret_is_base64: false
  - name: acl
    config:
      whitelist: ["reports.write"]
  - name: rate-limiting
    config:
      minute: 300
  - name: correlation-id
    config:
      header_name: "X-Correlation-Id"
      echo_downstream: true
```

## Practical interview talking points
- “Gateway validates JWT and scopes before traffic hits the service; the service still enforces JWT (layered defense).”
- “Rate limits and WAF live at the edge; retries are off for non-idempotent POST.”
- “We propagate correlationId/traceparent from the gateway so API → Kafka → Archive is traceable end-to-end.”
- “mTLS from gateway to services for service authentication; JWKS rotation handles key rollover.”
