package com.codexparas.ai_gateway.logging;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** AI Gateway-native event contract for Kafka, Logstash, and Elasticsearch. */
public record GatewayAuditEvent(
    String schemaVersion,
    String eventName,
    String eventId,
    Instant occurredAt,
    RequestContext request,
    ModelSelection model,
    Execution execution,
    GuardrailSummary guardrails,
    Usage usage,
    ErrorInfo error,
    Retention retention) {
  public record RequestContext(
      String requestId,
      String traceId,
      String spanId,
      String orgId,
      String appId,
      String method,
      String url,
      Map<String, String> headers,
      String body) {}

  public record ModelSelection(
      String requestedModel,
      String resolvedModel,
      String provider,
      String deployment,
      String region) {}

  public record Execution(
      String outcome,
      int httpStatus,
      long durationMs,
      Long upstreamDurationMs,
      int retryCount,
      boolean fallbackUsed,
      String upstreamRequestId,
      String streamTermination,
      Long timeToFirstTokenMs,
      Integer providerStatus,
      String providerErrorCode) {}

  public record GuardrailSummary(
      List<String> configured, List<String> evaluated, String decision, boolean blocked) {}

  public record Usage(
      Long inputTokens,
      Long outputTokens,
      Long totalTokens,
      Double estimatedCost,
      String currency) {}

  public record ErrorInfo(String type, String code, String message, boolean retryable) {}

  public record Retention(Instant purgeAt) {}
}
