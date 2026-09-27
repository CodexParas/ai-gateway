# AI Gateway Architecture

## Purpose

The gateway exposes provider-compatible HTTP protocols while centralizing authentication, model routing, configuration, guardrails, observability, and execution.

The gateway is implemented as a Servlet filter pipeline. Provider controllers are not part of the target request path. The request is resolved, validated, transformed, executed through Spring AI, and written back by gateway infrastructure.

## Design principles

- Use Servlet filters as the primary gateway boundary.
- Use `OncePerRequestFilter` for logging, authentication, validation, guardrails, model preparation, and execution boundaries.
- Resolve model configuration from Redis by requested model name.
- Resolve upstream credentials before model configuration so organization and application identity are trusted rather than accepted from arbitrary headers.
- Use Spring AI's `org.springframework.ai.chat.model.ChatModel` as the runtime model abstraction.
- Keep provider SDK details behind provider-specific factories and adapters.
- Keep gateway metadata separate from the Spring AI model instance.
- Apply request and response guardrails around model execution.
- Emit one structured, provider-neutral audit event per request or stream.
- Never log authorization headers, API keys, cookies, or provider credentials.

## Target request flow

```text
Incoming HTTP request
  -> Request logging boundary
  -> Upstream credential resolution
  -> Path/protocol route resolution
  -> Provider request validation
  -> Credential scope validation
  -> Model name extraction
  -> Redis model configuration lookup
  -> Model configuration validation
  -> Spring AI ChatModel preparation
  -> Request guardrails
  -> Spring AI request execution
  -> Response guardrails
  -> Provider-compatible response/SSE writing
  -> Completion logging and Kafka publication
```

The request enters through a provider-compatible path. The route determines the provider protocol and operation, but the model configuration remains the source of truth for the selected upstream model and provider settings.

## Servlet filter order

The intended order is:

| Order | Filter | Responsibility |
| --- | --- | --- |
| 0 | `RequestLoggingFilter` | Generate/propagate request ID, capture timing, publish completion audit event |
| 5 | `UpstreamCredentialFilter` | Extract and resolve API key/token from the incoming request |
| 10 | `RouteResolverFilter` | Resolve provider, operation, and streaming mode from the path and headers |
| 15 | `ModelNameFilter` | Extract the requested model from path or request body |
| 20 | `UpstreamCredentialScopeFilter` | Validate organization, application, scopes, and allowed models |
| 25 | `RequestValidatorFilter` | Run provider/protocol-specific request validation |
| 30 | `ModelConfigurationFilter` | Load and validate model configuration from Redis |
| 35 | `ChatModelFilter` | Resolve a provider factory and prepare Spring AI `ChatModel` |
| 40 | `GuardrailFilter` | Evaluate configured request-stage guardrails and transform/block the body |
| 50 | `RequestExecutionFilter` | Convert the request to a Spring AI `Prompt` and execute the model |
| 60 | `ResponseGuardrailFilter` | Inspect, redact, transform, or block model output |
| 70 | `GatewayResponseFilter` | Write the provider-compatible response or SSE stream |

Logging surrounds the complete chain. It must also register Servlet async listeners so streaming requests are logged when they complete, time out, fail, or are cancelled.

## Route resolution

Routes are normalized into a gateway-neutral representation:

```java
public record GatewayRoute(
    String provider,
    String operation,
    boolean streaming
) {}
```

Canonical examples:

| Incoming path | Provider | Operation |
| --- | --- | --- |
| `/openai/chat/completions` | `OPENAI` | chat completion |
| `/openai/responses` | `OPENAI` | response |
| `/anthropic/messages` | `ANTHROPIC` | messages |
| `/google/models/{model}:generateContent` | `GOOGLE` | generate content |
| `/bedrock/models/{model}/converse` | `BEDROCK` | converse |
| `/azure/chat/completions` | `AZURE` | chat completion |

The resolver must reject unknown paths and ambiguous provider routes before model execution.

## Upstream authentication and identity

The gateway accepts credentials from the upstream service using supported request forms such as:

```text
Authorization: Bearer <token>
Authorization: ApiKey <api-key>
X-API-Key: <api-key>
```

The credential is hashed before Redis lookup. Redis stores credential records in a hash keyed by the credential hash.

The resolved credential supplies trusted gateway identity:

```java
public record UpstreamCredential(
    String credentialId,
    String orgId,
    String appId,
    boolean enabled,
    Instant expiresAt,
    Set<String> scopes,
    Set<String> allowedModels
) {}
```

The gateway uses the resolved `orgId` and `appId` for authorization and logging. Incoming `x-org-id` and `x-app-id` headers are metadata only and must not override credential identity.

## Model configuration

The requested model name is used as the Redis hash field. The stored value is JSON and is parsed, schema-validated, and domain-validated before model preparation.

Conceptual configuration:

```json
{
  "provider": "OPENAI",
  "model": "gpt-5",
  "url": "https://api.openai.com",
  "region": null,
  "deployment": null,
  "apiVersion": null,
  "auth": {
    "type": "API",
    "config": {}
  },
  "request": {},
  "guardrails": []
}
```

Required behavior:

- Missing Redis entry raises a model-not-found error.
- Redis or parsing failure raises a model-configuration lookup error.
- Invalid provider configuration raises a validation error.
- Provider, model, authentication mode, and provider-specific fields are validated before model creation.
- Credentials and secrets must never be copied into logs or error responses.

## Spring AI model preparation

`ChatModelPrepare` prepares Spring AI's model abstraction:

```java
org.springframework.ai.chat.model.ChatModel
```

The gateway stores runtime metadata separately:

```java
public record PreparedChatModel(
    ChatModel chatModel,
    String requestedModel,
    String provider,
    String model,
    String url,
    String region,
    String deployment,
    String apiVersion,
    JsonNode auth,
    JsonNode request,
    JsonNode guardrails
) {}
```

Provider creation is delegated through:

```java
public interface ChatModelProviderFactory {

    String provider();

    ChatModel create(JsonNode configuration);
}
```

Factories currently exist for OpenAI, Anthropic, Bedrock Converse, Google GenAI, and Azure/OpenAI-compatible routing. Provider-specific SDK construction must remain inside these factories.

The actual Spring AI model is placed in the request attribute used by execution and is bound to `ChatModelContext` for synchronous downstream components. Async and streaming work must capture the model explicitly instead of relying on a ThreadLocal after the Servlet thread returns.

## Provider request and response adapters

Provider protocol payloads are not passed directly to provider SDKs from filters. The execution layer uses adapters:

```text
OpenAiRequestAdapter
AnthropicRequestAdapter
BedrockRequestAdapter
GoogleRequestAdapter
AzureRequestAdapter
```

Request adapters convert the incoming protocol payload into Spring AI `Prompt` and `ChatOptions` values. Response adapters convert `ChatResponse` and streamed response chunks back into the protocol expected by the upstream service.

This keeps the filter pipeline provider-neutral while preserving OpenAI, Anthropic, Google, Bedrock, and Azure wire compatibility.

## Guardrails

Guardrails are configured per model and resolved through `GuardrailFactory`.

The guardrail contract is:

```java
public interface Guardrail {

    Set<String> supportedTypes();

    GuardrailResult inspect(JsonNode definition, String body);

    List<String> validate(JsonNode definition, String path);
}
```

Current implementations include:

- PII redaction
- DLP/secret detection
- Prompt injection detection
- Content safety/toxicity detection
- JSON schema/custom no-op extension points

Guardrail stages:

```text
REQUEST -> inspect and optionally transform/block input
RESPONSE -> inspect and optionally transform/block output
BOTH -> run at both stages
```

For streams, response guardrails must process chunks or an explicitly bounded aggregate. The implementation must define behavior for a violation after partial output has already been sent.

## Execution and streaming

The execution filter is responsible for selecting the execution mode:

```text
Non-streaming:
  request -> Prompt -> ChatModel.call(prompt) -> response adapter -> HTTP response

Streaming:
  request -> Prompt -> ChatModel.stream(prompt) -> chunk guardrails -> SSE writer
```

Streaming requirements:

- Use Servlet async processing.
- Preserve request, trace, organization, application, and model metadata for the stream lifetime.
- Propagate client disconnect and cancellation to the model publisher.
- Emit provider-compatible SSE event names and terminal events.
- Record time-to-first-token and total stream duration.
- Publish exactly one completion audit event.

## Logging and audit pipeline

The logging flow is:

```text
Gateway request
  -> RequestLoggingFilter
  -> structured GatewayAuditEvent
  -> Kafka topic
  -> Logstash
  -> Elasticsearch
```

The event includes:

- request, trace, and span IDs
- trusted organization and application identity
- route and provider
- requested and resolved model
- HTTP status and outcome
- latency and streaming metrics
- guardrail configuration and decision
- token usage and estimated cost when available
- sanitized error information
- retention metadata

Authorization headers, cookies, API keys, tokens, and raw provider credentials are excluded.

## Error handling

Expected gateway failures use domain-specific exceptions and a consistent error payload. The filter boundary must handle errors that occur before controller invocation because `@RestControllerAdvice` only covers controller/handler execution reliably.

Error categories include:

- invalid route
- missing or invalid upstream credentials
- insufficient scope or disallowed model
- model configuration not found
- model configuration invalid
- provider model unavailable
- request validation failure
- guardrail violation
- provider execution failure
- stream timeout or client cancellation

Responses must not expose stack traces, secrets, Redis values, provider credentials, or internal implementation identifiers.

## Target package organization

```text
com.codexparas.ai_gateway
  filters/
  model/
    provider/
  guardrail/
    impl/
  security/
  logging/
  route/
  execution/
    adapter/
  exception/
```

## Implementation status

Already implemented:

- Servlet filter foundation
- credential resolution and scope checks
- Redis model lookup and validation
- Spring AI model abstraction and provider factory implementations
- request guardrail implementations
- structured Kafka logging event
- async-aware request logging
- global controller exception handler

Still required for the complete gateway path:

- route resolver
- provider request validators
- request-to-`Prompt` adapters
- `RequestExecutionFilter`
- response adapters and provider-compatible writers
- response guardrail filter
- controller-free catch-all routing
- filter-level exception boundary
- dynamic per-model provider credential/options construction
- end-to-end and streaming tests
