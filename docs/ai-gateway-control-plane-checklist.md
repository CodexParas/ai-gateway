# Configurator Service Control-Plane Checklist

This checklist belongs to the separate configurator service. It tracks the
control plane described in `docs/ai-gateway-control-plane-model.md`, including
authoring, governance, secret management, and runtime configuration
materialization. These capabilities are not responsibilities of the AI
Gateway.

Legend:

- `[x]` Implemented
- `[~]` Partially implemented or requires completion
- `[ ]` Pending

## 1. Control-plane domain model

- [ ] Define the model record identity and lifecycle state.
- [ ] Define provider, model, endpoint, and deployment fields.
- [ ] Define display name, owner, tenant, environment, and metadata fields.
- [ ] Define aliases and weighted routing rules.
- [ ] Define fallback model references as a graph with cycle detection.
- [ ] Define credential references without storing provider secrets in the model record.
- [ ] Define request defaults and guardrail policy references.
- [ ] Define resilience, rate, token, and concurrency limits.
- [ ] Define observability and payload-retention policies.

## 2. Authoring and validation

- [ ] Provide create, update, read, and delete operations for model records.
- [ ] Validate common control-plane fields.
- [ ] Validate provider-specific endpoint and authentication requirements.
- [ ] Validate guardrails against the runtime guardrail contract.
- [ ] Validate aliases, weights, and fallback references.
- [ ] Reject fallback cycles and references to disabled models.
- [ ] Validate limits and resilience values against platform bounds.
- [ ] Prevent secrets, raw API keys, and tokens from being persisted in records.
- [ ] Produce deliberate, field-level validation errors.

## 3. Approval and lifecycle governance

- [ ] Add draft, review, approved, published, disabled, and retired states.
- [ ] Define who may create, approve, publish, disable, and retire configurations.
- [ ] Capture an immutable change history and actor identity.
- [ ] Require approval for production credential, routing, and guardrail changes.
- [ ] Support scheduled activation and expiration where required.
- [ ] Define rollback to a previous approved version.
- [ ] Add optimistic locking or equivalent concurrent-edit protection.

## 4. Secret resolution

- [ ] Integrate with the deployment secret manager.
- [ ] Resolve `credentialRef` only during controlled materialization.
- [ ] Support provider-specific credential shapes and authentication modes.
- [ ] Keep resolved credentials out of API responses, logs, and audit payloads.
- [ ] Define secret rotation and cache invalidation behavior.
- [ ] Define failure behavior when a referenced secret is missing or unavailable.
- [ ] Ensure secret resolution is scoped to the target organization, application,
  and environment.

## 5. Runtime materialization

- [ ] Map `endpoint` to runtime `url`, `region`, `deployment`, and `apiVersion`.
- [ ] Map `requestDefaults` to runtime `request`.
- [ ] Map guardrail definitions to runtime `guardrails`.
- [ ] Resolve fallback references to the runtime fallback representation.
- [ ] Preserve the model name in both the Redis hash field and runtime `model`.
- [ ] Write only the runtime schema accepted by
  `ai-gateway-provider-config.schema.json`.
- [ ] Publish a version, checksum, or updated-at marker with each materialization.
- [ ] Make materialization idempotent.
- [ ] Support atomic publication of a new Redis configuration set.
- [ ] Define stale-configuration detection and reconciliation.

## 6. Redis distribution and cache behavior

- [ ] Define the Redis hash name and environment/tenant partitioning strategy.
- [ ] Define TTL or persistence policy for materialized model configurations.
- [ ] Add configuration version checks for gateway readers.
- [ ] Support refresh, invalidation, and warm-up operations.
- [ ] Define behavior during Redis outages or partial publication.
- [ ] Prevent cross-tenant model and credential reads.
- [ ] Add metrics for materialization success, failure, age, and drift.

## 7. Configurator-side policy preparation

- [ ] Resolve aliases to concrete model records.
- [ ] Apply weighted routing deterministically and observably.
- [ ] Publish only enabled models and valid routing targets.
- [ ] Materialize request, timeout, retry, and limit settings for the gateway
  runtime contract.
- [ ] Materialize an ordered fallback chain with loop and scope protection.
- [ ] Preserve routing and policy decisions in configurator audit events.

## 8. Observability and operations

- [ ] Emit audit events for authoring, approval, publication, rollback, and retirement.
- [ ] Expose configuration version and publication health metrics.
- [ ] Add drift detection between the control-plane record and Redis materialization.
- [ ] Add alerts for failed publication, stale configuration, and secret failures.
- [ ] Apply payload logging and retention policy without exposing sensitive content.
- [ ] Publish configuration identity so gateway request and execution logs can
  correlate to the materialized version.
- [ ] Provide an operator view of active models, versions, routes, and dependencies.

## 9. API and integration boundaries

- [ ] Define a versioned control-plane API.
- [ ] Define authentication and authorization for control-plane operations.
- [ ] Define idempotency rules for publish and rollback operations.
- [ ] Define events for model-created, model-approved, model-published,
  model-disabled, and model-rolled-back transitions.
- [ ] Define the materializer contract and retry/dead-letter behavior.
- [ ] Keep the gateway dependent on the runtime Redis contract, not control-plane
  persistence details.

## 10. Verification

- [ ] Unit-test domain validation and lifecycle transitions.
- [ ] Unit-test secret-reference handling and redaction.
- [ ] Unit-test runtime materialization mappings.
- [ ] Test fallback graph validation and deterministic routing.
- [ ] Test atomic publication and rollback behavior.
- [ ] Test Redis outage, stale data, and reconciliation scenarios.
- [ ] Test tenant and organization isolation.
- [ ] Test end-to-end author → publish → materialize flow.
- [ ] Test audit completeness and sensitive-data exclusion.

## Service boundary

The configurator owns the following control-plane concerns:

- `displayName`, `aliases`, and weighted routing;
- `credentialRef` and secret rotation;
- approval, version, publication, and rollback state;
- timeout, rate, token, and concurrency limits;
- observability sampling and retention policy;
- multi-model fallback references;
- ownership, tenancy, environment, and administrative metadata.

The configurator publishes only the runtime shape accepted by
`ai-gateway-provider-config.schema.json`. The AI Gateway consumes that shape
and performs request-time execution; it does not read configurator records or
resolve control-plane secrets.
