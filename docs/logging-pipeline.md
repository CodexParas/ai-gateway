# Gateway logging pipeline

The gateway emits one structured JSON audit event per request:

```text
AI Gateway -> Kafka (ai-gateway.logs) -> Logstash -> Elasticsearch
```

The event is keyed by `request.requestId` and uses the AI Gateway event schema:

- `request`: correlation IDs, tenant/application IDs, HTTP metadata, safe headers, and optional request body
- `model`: requested/resolved model, provider, deployment, and region
- `execution`: outcome, HTTP status, total duration, retry count, and fallback usage
- `guardrails`: configured/evaluated policies and decision
- `usage`: input/output/total tokens and estimated cost, when the provider reports them
- `error`: sanitized failure information
- `retention`: purge timestamp

The event name is `ai.gateway.request.completed` and the schema version is `1.0`.

## Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

ai:
  gateway:
    logging:
      enabled: true
      topic: ai-gateway.logs
      system-name: API-GATEWAY
      include-request-body: false
      include-response-body: false
      purge-days: 30
```

Sensitive request headers (`authorization`, `cookie`, and `set-cookie`) are always excluded. Request-body logging is disabled by default and should only be enabled after confirming the payload is safe to retain.

The Logstash pipeline is at `deploy/logstash/pipeline/ai-gateway-logs.conf`. Set `KAFKA_BOOTSTRAP_SERVERS`, `ELASTICSEARCH_HOSTS`, and optional Elasticsearch credentials in the Logstash environment.

## Upstream authentication

The gateway resolves caller credentials before model configuration lookup. It accepts:

- `Authorization: Bearer <token>`
- `Authorization: ApiKey <token>`
- `X-API-Key: <token>`

Only the SHA-256 token hash is used as the Redis hash field. The raw token is never logged or stored in the request context.

```bash
TOKEN_HASH=$(printf '%s' "$UPSTREAM_TOKEN" | shasum -a 256 | cut -d ' ' -f 1)
redis-cli HSET ai-gateway:credentials "$TOKEN_HASH" \
  '{"credentialId":"svc-orders","orgId":"org-123","appId":"orders","enabled":true,"scopes":["inference"],"allowedModels":["gpt-5"]}'
```

The resolved `orgId` and `appId` are used for audit events. Credential records can restrict models with `allowedModels` and must include the `inference` scope when scopes are configured.
