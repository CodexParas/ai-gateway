# AI Gateway Implementation Checklist

This checklist tracks the agreed controller-free Servlet gateway implementation.

Legend:

- `[x]` Implemented
- `[~]` Partially implemented or requires completion
- `[ ]` Pending

## 1. Gateway filter pipeline

- [x] Logging filter surrounds the gateway request lifecycle.
- [x] Request ID generation and response propagation.
- [x] Async request completion listener for Servlet/SSE requests.
- [x] Upstream credential extraction from bearer, API-key, and `X-API-Key` headers.
- [x] Credential hash lookup in Redis.
- [x] Credential status and expiry validation.
- [x] Organization and application identity resolution from credentials.
- [x] Credential scope and allowed-model validation.
- [x] Simple `/{provider}/{operation}` route/protocol resolution filter.
- [x] Provider request validation filter.
- [x] Model name extraction and Redis lookup filter.
- [x] Combined model-name, Redis lookup, parsing, and configuration-validation filter.
- [x] Spring AI `ChatModel` preparation filter.
- [x] Request guardrail filter.
- [x] Request transformation filter/service SPI.
- [x] Spring AI request execution filter.
- [x] Response transformation filter/service.
- [x] Response guardrail filter.
- [x] Provider-compatible response writer.
- [x] Streaming/SSE response writer.
- [x] Completion logging and Kafka publication.
- [x] Filter-level exception boundary.

## 2. Route and protocol resolution

- [x] Define `GatewayRoute` with provider, operation, and streaming mode.
- [x] Resolve provider from incoming path and protocol.
- [x] Resolve operation from path and HTTP method.
- [x] Detect streaming mode from request body, query parameters, or headers.
- [x] Reject unsupported or ambiguous paths.
- [x] Preserve route metadata for logging and execution.

Expected routes include:

- [x] OpenAI-compatible chat completions.
- [x] OpenAI responses.
- [x] Anthropic messages.
- [x] Google GenAI generate content.
- [x] Bedrock Converse.
- [x] Azure/OpenAI-compatible chat completions.

## 3. Model resolution and configuration

- [x] Extract requested model from request path or body.
- [x] Combine model extraction with Redis model configuration lookup.
- [x] Resolve Redis hash entry by model name.
- [x] Raise a model-not-found error when the hash field is absent.
- [x] Parse stored JSON configuration.
- [x] Run model configuration validators.
- [x] Reject malformed or invalid model configuration.
- [x] Make model resolution route-aware where the protocol uses path model identifiers.
- [x] Ensure credential scope validation runs after model resolution.
- [x] Apply per-model endpoint, region, deployment, and API version settings.
- [x] Apply per-model authentication settings to provider model creation.
- [x] Apply per-model request options to the Spring AI model.

## 4. Provider request validation

- [x] Define provider validator SPI.
- [x] Add OpenAI request validator.
- [x] Add Anthropic request validator.
- [x] Add Bedrock request validator.
- [x] Add Google request validator.
- [x] Add Azure request validator.
- [x] Validate required protocol fields.
- [x] Validate path/model consistency.
- [x] Validate streaming flags.
- [x] Validate supported message/content formats.
- [x] Validate provider-specific options.
- [x] Return consistent validation errors.

## 5. Spring AI model preparation

- [x] Use `org.springframework.ai.chat.model.ChatModel` as the runtime abstraction.
- [x] Keep gateway metadata in `PreparedChatModel`.
- [x] Bind the actual Spring AI model to the request attribute.
- [x] Bind the actual Spring AI model to `ChatModelContext` for synchronous execution.
- [x] Define `ChatModelProviderFactory`.
- [x] Add OpenAI provider factory.
- [x] Add Anthropic provider factory.
- [x] Add Bedrock Converse provider factory.
- [x] Add Google GenAI provider factory.
- [x] Add Azure/OpenAI-compatible provider factory.
- [x] Create provider models dynamically from Redis configuration.
- [x] Avoid relying on ThreadLocal state across asynchronous stream callbacks.
- [x] Capture model and request context explicitly for asynchronous execution.

## 6. Request transformers

- [x] Define `RequestTransformer` SPI.
- [x] Define `TransformedChatRequest`.
- [x] Transform incoming protocol messages into Spring AI `Message` objects.
- [x] Transform incoming protocol options into Spring AI `ChatOptions`.
- [~] Preserve system, user, assistant, tool, and multimodal content where supported.
- [~] Preserve temperature, max tokens, top-p, stop sequences, tools, and response format.
- [x] Add OpenAI request transformer.
- [x] Add Anthropic request transformer.
- [x] Add Bedrock request transformer.
- [x] Add Google request transformer.
- [x] Add Azure request transformer.
- [x] Validate unsupported options before execution.

Target transformation:

```text
Provider request JSON
  -> RequestTransformer
  -> List<Message> + ChatOptions
  -> Prompt
```

## 7. Spring AI request execution

- [x] Define execution context containing route, credentials, prepared model, and transformed request.
- [x] Implement non-streaming execution using `ChatModel.call(prompt)`.
- [x] Implement streaming execution using `ChatModel.stream(prompt)`.
- [x] Capture provider response metadata.
- [x] Capture token usage when available.
- [x] Capture provider request/correlation IDs.
- [x] Apply timeout and cancellation behavior.
- [x] Map provider exceptions to gateway exceptions.
- [x] Track time-to-first-token and total stream duration.

## 8. Response transformers

- [x] Define `ResponseTransformer` SPI.
- [x] Define `StreamingResponseTransformer` SPI.
- [x] Transform Spring AI `ChatResponse` into OpenAI-compatible response format.
- [x] Transform Spring AI `ChatResponse` into Anthropic response format.
- [x] Transform Spring AI `ChatResponse` into Bedrock response format.
- [x] Transform Spring AI `ChatResponse` into Google response format.
- [x] Transform Spring AI `ChatResponse` into Azure response format.
- [~] Transform usage metadata into provider-compatible usage fields.
- [ ] Transform tool calls and finish reasons completely.
- [x] Define consistent behavior when provider metadata is unavailable.

Target transformation:

```text
ChatResponse / streamed ChatResponse
  -> ResponseTransformer
  -> Provider response JSON / SSE event
```

## 9. Response guardrails

- [x] Add response-stage guardrail execution.
- [x] Support response redaction.
- [x] Support response blocking.
- [x] Support response transformation.
- [x] Apply response guardrails to non-streaming responses.
- [x] Apply response guardrails to streaming chunks or bounded aggregates.
- [~] Define behavior when a stream is blocked after partial output.
- [x] Include response guardrail results in the audit event.

## 10. Provider-compatible response writing

- [x] Write non-streaming JSON responses.
- [x] Write provider-compatible response headers.
- [x] Write provider request IDs.
- [~] Write provider-compatible SSE event names.
- [x] Emit stream completion events.
- [~] Emit sanitized stream error events.
- [~] Handle client disconnects.
- [ ] Cancel upstream model execution on disconnect.
- [x] Ensure exactly one completion audit event per request.

## 11. Error handling

- [x] Global controller exception handler exists.
- [x] Add filter-level gateway exception boundary.
- [x] Map route errors to consistent responses.
- [x] Map credential errors to consistent responses.
- [x] Map model lookup and validation errors.
- [x] Map unsupported provider/model errors.
- [x] Map request validation errors.
- [x] Map guardrail violations.
- [~] Map provider execution errors.
- [~] Map stream timeout, cancellation, and disconnect errors.
- [x] Prevent secrets and stack traces from reaching clients.

## 12. Logging and audit completeness

- [x] Structured gateway audit event.
- [x] Kafka publisher.
- [x] Logstash pipeline documentation/configuration.
- [x] Sanitized request headers.
- [x] Trusted organization/application identity in logs.
- [x] Model and provider metadata in logs.
- [x] Guardrail decision summary in logs.
- [x] Provider request ID in logs.
- [x] Token usage in logs.
- [x] Time-to-first-token in logs.
- [x] Stream cancellation state in logs.
- [ ] Retry and fallback metadata in logs.
- [ ] Final provider status/error code in logs.

## 13. Controller-free gateway transition

- [ ] Add catch-all gateway routing entry point.
- [ ] Route all supported provider paths through the filter pipeline.
- [ ] Remove provider-specific controller dependency from the target execution path.
- [ ] Retain provider services only as adapters where still required.
- [ ] Remove or deprecate duplicate controller execution paths.

## 14. Testing

- [x] Route resolution unit tests.
- [x] Model resolution and validation tests.
- [x] Credential and scope tests.
- [ ] Provider request validator tests.
- [ ] Provider factory tests.
- [x] Request transformer tests.
- [ ] Non-streaming execution tests.
- [x] Response transformer tests.
- [x] Request guardrail tests.
- [ ] Response guardrail tests.
- [ ] SSE chunk and cancellation tests.
- [ ] Filter ordering tests.
- [ ] Filter-level exception tests.
- [ ] End-to-end controller-free gateway tests.
