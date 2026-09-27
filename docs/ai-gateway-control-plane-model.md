# AI Gateway control-plane model

The gateway currently reads a provider model configuration from a Redis hash:

```text
HGET <model-config-hash> <requested-model-name>
```

The hash field is the model identity. The JSON stored as the hash value is
defined by
`src/main/resources/schema/ai-gateway-provider-config.schema.json`.
That schema is intentionally limited to fields the runtime currently reads:
provider selection, provider credentials, request defaults, fallback model,
and guardrails.

The implementation work for this future layer is tracked in
`docs/ai-gateway-control-plane-checklist.md`.

## Future control-plane responsibility

A broader control plane can own the lifecycle and governance of model
configurations without making the request gateway responsible for authoring
or policy management. A future model record may contain:

```json
{
  "id": "openai-gpt-5-prod",
  "displayName": "GPT-5 production",
  "enabled": true,
  "provider": "OPENAI",
  "model": "gpt-5",
  "endpoint": {
    "url": "https://api.openai.com/v1",
    "region": null,
    "deployment": null,
    "apiVersion": null
  },
  "credentialRef": "secret://ai/openai/prod",
  "routing": {
    "aliases": ["default-chat"],
    "weight": 100,
    "fallbackModelRefs": ["openai-gpt-5-mini-prod"]
  },
  "resilience": {
    "maxAttempts": 2,
    "timeoutMs": 30000,
    "retryableErrors": ["429", "500", "502", "503", "504"]
  },
  "limits": {
    "maxInputTokens": 128000,
    "maxOutputTokens": 8192,
    "requestsPerMinute": 600
  },
  "observability": {
    "logPayloads": false,
    "publishUsage": true,
    "samplingRate": 1.0
  },
  "requestDefaults": {
    "temperature": 0.2
  },
  "guardrails": [],
  "metadata": {
    "environment": "production",
    "owner": "platform"
  }
}
```

This is a control-plane contract, not a runtime Redis value. In particular,
`credentialRef` is a reference and must never be passed directly to a
provider factory. The control plane or a materialization worker must resolve
it through the configured secret manager, validate the resulting credentials,
and write only the runtime shape to Redis. The current runtime expects
resolved credential strings such as `auth.apiKey`.

## Materialization boundary

The intended lifecycle is:

```text
author -> validate -> approve/publish -> resolve secrets -> materialize Redis
       -> gateway reads model config -> execute -> audit/log
```

Materialization should:

1. Validate provider-specific fields and guardrail definitions.
2. Resolve `credentialRef` into an ephemeral or encrypted deployment value.
3. Map `endpoint` to runtime `url`, `region`, `deployment`, and `apiVersion`.
4. Map `requestDefaults` to runtime `request`.
5. Resolve fallback references into the runtime `request.fallbackModel` chain.
6. Preserve the model name in both the Redis hash field and runtime `model`.
7. Publish a version, checksum, or updated-at marker for safe rollout and
   rollback.

## Fields that are future-only today

The gateway does not currently consume these control-plane concerns directly:

- display names, aliases, weighted routing, and provider selection policies;
- secret references and secret rotation workflows;
- per-model timeout, retry, rate, token, and concurrency limits;
- rollout, approval, versioning, and rollback state;
- observability sampling and payload-retention policies;
- multi-model fallback chains beyond the current runtime fallback model;
- ownership, tenancy, environment, and administrative metadata.

Those fields should remain in the control plane until the gateway has an
explicit runtime adapter for them. Keeping this boundary prevents unused
configuration from appearing supported while allowing the control plane to
evolve independently.
