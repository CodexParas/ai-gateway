# AI Gateway

A Spring Boot gateway that exposes provider-compatible HTTP APIs (OpenAI, Anthropic, Bedrock, Google, Azure) and routes each request to a configured upstream model. It centralizes authentication, model routing, guardrails, observability, and execution behind a Servlet filter pipeline.

## How it works

```text
Request -> logging -> upstream credential resolution -> route resolution
        -> request validation -> credential scope check -> model name extraction
        -> Redis model config lookup -> request guardrails -> Spring AI execution
        -> response guardrails -> provider-compatible response / SSE
        -> completion logging + Kafka event
```

- **Provider endpoints:** `/openai/*`, `/anthropic/*`, `/bedrock/*`, `/google/*`, `/azure/*`
- **Model config:** stored in a Redis hash, keyed by requested model name (schema in `src/main/resources/schema/`)
- **Execution:** Spring AI `ChatModel`, with request/response transformers per provider
- **Fallbacks:** scoped non-stream fallback to a configured fallback model
- **Observability:** structured audit events and logs published to Kafka

## Requirements

- Java 21
- Redis (model configuration and upstream credentials)
- Kafka (optional, for events and logs)

## Run

```bash
./mvnw spring-boot:run
```

## Configuration

Set via environment variables (defaults in `src/main/resources/application.yaml`):

| Variable | Default | Purpose |
| --- | --- | --- |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis connection |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka connection |
| `AI_GATEWAY_SECURITY_ENABLED` | `true` | Require upstream credentials |
| `AI_GATEWAY_MODEL_CONFIG_REDIS_HASH` | `ai-gateway:model-config` | Model config hash |
| `AI_GATEWAY_CREDENTIAL_HASH` | `ai-gateway:credentials` | Credential hash |
| `AI_GATEWAY_KAFKA_ENABLED` | `false` | Publish audit events |
| `AI_GATEWAY_LOGGING_ENABLED` | `true` | Request logging |
| `AI_GATEWAY_STREAMING_TIMEOUT` | `5m` | SSE stream timeout |

Authorization headers, API keys, cookies, and provider credentials are never logged.

## Docs

- [Gateway architecture](docs/gateway-architecture.md)
- [Control-plane model](docs/ai-gateway-control-plane-model.md)
- [Execution checklist](docs/ai-gateway-execution-checklist.md)
- [Implementation checklist](docs/gateway-implementation-checklist.md)
- [Logging pipeline](docs/logging-pipeline.md)
