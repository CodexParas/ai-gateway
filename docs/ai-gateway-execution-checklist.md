# AI Gateway Execution Checklist

This checklist defines the AI Gateway boundary after the separate configurator
service has published materialized provider configuration. The gateway owns
request-time authentication, runtime configuration lookup, provider execution,
guardrails, fallback execution, responses, and execution observability.

The detailed implementation tracker is
`docs/gateway-implementation-checklist.md`. This document records the boundary
contract and the gateway-specific responsibilities that must remain true as the
configurator evolves.

Legend:

- `[x]` Implemented
- `[~]` Partially implemented or requires completion
- `[ ]` Pending

## 1. Runtime configuration contract

- [x] Read materialized model configuration from Redis by model name.
- [x] Validate the stored value against
  `ai-gateway-provider-config.schema.json` and runtime domain validators.
- [x] Preserve the Redis hash field/model-name equality requirement.
- [ ] Define the published configuration version/checksum marker consumed by
  gateway readers.
- [ ] Define gateway behavior for stale, missing, malformed, or partially
  published configuration.
- [ ] Define refresh, cache invalidation, and warm-up behavior if local caching
  is introduced.
- [ ] Ensure organization, application, environment, and model scope cannot be
  crossed during lookup.

## 2. Request-time execution

- [x] Authenticate the caller and resolve organization/application scope.
- [x] Resolve the requested model configuration.
- [x] Prepare the provider-specific Spring AI model.
- [x] Apply runtime endpoint, authentication, request, and guardrail settings.
- [x] Execute non-streaming and streaming requests.
- [x] Transform provider requests and responses at the protocol boundary.
- [x] Handle provider errors, timeouts, cancellation, and client disconnects.

## 3. Runtime policy enforcement

- [x] Enforce enabled model state at request time.
- [x] Enforce caller model scope.
- [x] Apply configured request and response guardrails.
- [x] Execute configured fallback models with loop and scope protection.
- [ ] Enforce materialized per-model timeout, rate, token, and concurrency
  limits when those fields are added to the runtime contract.
- [ ] Apply materialized retryable-provider-error policy when it is added to
  the runtime contract.
- [ ] Apply aliases and weighted routing only after the configurator publishes
  an explicit gateway runtime representation.

## 4. Secrets and data handling

- [x] Consume only the resolved runtime authentication shape required by the
  provider adapter.
- [x] Prevent credentials and sensitive configuration from API responses,
  logs, and audit payloads.
- [ ] Define behavior when the configurator publishes an unusable or expired
  credential materialization.
- [ ] Keep the gateway independent of configurator secret-manager APIs.

## 5. Execution observability

- [x] Emit request and execution audit events.
- [x] Record selected model, provider, route, guardrail, fallback, and provider
  response metadata where available.
- [x] Record token usage, timing, cancellation, and final status/error data.
- [ ] Include the configurator-published configuration version in gateway audit
  events and metrics.
- [ ] Expose metrics for configuration age, lookup failures, and runtime drift
  signals supplied by the distribution layer.

## 6. Gateway integration boundary

- [ ] Depend only on the published Redis/runtime configuration contract.
- [ ] Do not implement model authoring, approval, publication, rollback, or
  control-plane record persistence.
- [ ] Do not resolve `credentialRef` or call the configurator's secret manager.
- [ ] Do not expose aliases, routing weights, administrative metadata, or
  lifecycle state unless the configurator materializes them into an explicit
  runtime contract.
- [x] Keep provider SDK construction behind gateway provider adapters.
- [x] Keep execution errors and responses within the gateway's public contract.

## Ownership flow

```text
Configurator service:
  author → validate → approve/publish → resolve secrets → materialize Redis

AI Gateway:
  authenticate → resolve runtime config → prepare provider → execute
  → enforce runtime guardrails/fallbacks → write response → audit
```
