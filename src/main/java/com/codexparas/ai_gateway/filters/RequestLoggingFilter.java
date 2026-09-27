package com.codexparas.ai_gateway.filters;

import com.codexparas.ai_gateway.config.GatewayLoggingProperties;
import com.codexparas.ai_gateway.guardrail.GuardrailDecision;
import com.codexparas.ai_gateway.logging.GatewayAuditEvent;
import com.codexparas.ai_gateway.logging.GatewayAuditPublisher;
import com.codexparas.ai_gateway.model.PreparedChatModel;
import com.codexparas.ai_gateway.security.UpstreamCredential;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final String REQUEST_ID = "x-request-id";
  private static final String TRACE_ID = "x-trace-id";
  private static final String SPAN_ID = "x-span-id";
  private static final String ORG_ID = "x-org-id";
  private static final String APP_ID = "x-app-id";
  private static final String STARTED = RequestLoggingFilter.class.getName() + ".started";
  private static final String PUBLISHED = RequestLoggingFilter.class.getName() + ".published";

  private final GatewayAuditPublisher auditPublisher;
  private final GatewayLoggingProperties properties;

  public RequestLoggingFilter(
      GatewayAuditPublisher auditPublisher, GatewayLoggingProperties properties) {
    this.auditPublisher = auditPublisher;
    this.properties = properties;
  }

  private static String headerOrGenerate(HttpServletRequest request) {
    String value = request.getHeader(RequestLoggingFilter.REQUEST_ID);
    return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
  }

  private static Map<String, String> safeHeaders(HttpServletRequest request) {
    Map<String, String> safe = new LinkedHashMap<>();
    java.util.Collections.list(request.getHeaderNames())
        .forEach(
            name -> {
              if (!name.equalsIgnoreCase("authorization")
                  && !name.equalsIgnoreCase("cookie")
                  && !name.equalsIgnoreCase("set-cookie")) {
                safe.put(name, request.getHeader(name));
              }
            });
    return Map.copyOf(safe);
  }

  private static String safeMessage(Throwable error) {
    String message = error.getMessage();
    return message == null
        ? error.getClass().getSimpleName()
        : message.substring(0, Math.min(message.length(), 500));
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    Instant started = Instant.now();
    request.setAttribute(STARTED, started);
    String requestId = headerOrGenerate(request);
    response.setHeader(REQUEST_ID, requestId);
    try {
      chain.doFilter(request, response);
    } catch (IOException | ServletException | RuntimeException exception) {
      request.setAttribute(GatewayRequestAttributes.ERROR, exception);
      throw exception;
    } finally {
      if (request.isAsyncStarted()) {
        request.getAsyncContext().addListener(new CompletionListener(request, response, requestId));
      } else {
        publishOnce(request, response, requestId, started);
      }
    }
  }

  private void publishOnce(
      HttpServletRequest request, HttpServletResponse response, String requestId, Instant started) {
    if (request.getAttribute(PUBLISHED) != null) return;
    request.setAttribute(PUBLISHED, Boolean.TRUE);
    Instant completed = Instant.now();
    PreparedChatModel chatModel =
        (PreparedChatModel) request.getAttribute(GatewayRequestAttributes.PREPARED_CHAT_MODEL);
    UpstreamCredential credential =
        (UpstreamCredential) request.getAttribute(GatewayRequestAttributes.UPSTREAM_CREDENTIAL);
    Throwable error = (Throwable) request.getAttribute(GatewayRequestAttributes.ERROR);
    int httpStatus = response.getStatus();
    String outcome = error == null && httpStatus < 400 ? "SUCCESS" : "FAILURE";
    GuardrailDecision decision =
        (GuardrailDecision) request.getAttribute(GatewayRequestAttributes.GUARDRAIL_DECISION);
    List<String> configured = decision == null ? List.of() : decision.configured();
    GatewayAuditEvent event =
        new GatewayAuditEvent(
            "1.0",
            "ai.gateway.request.completed",
            UUID.randomUUID().toString(),
            completed,
            new GatewayAuditEvent.RequestContext(
                requestId,
                request.getHeader(TRACE_ID),
                request.getHeader(SPAN_ID),
                credential == null ? request.getHeader(ORG_ID) : credential.orgId(),
                credential == null ? request.getHeader(APP_ID) : credential.appId(),
                request.getMethod(),
                request.getRequestURL().toString(),
                safeHeaders(request),
                properties.includeRequestBody()
                    ? (String) request.getAttribute(GatewayRequestAttributes.REQUEST_BODY)
                    : null),
            new GatewayAuditEvent.ModelSelection(
                chatModel == null
                    ? (String) request.getAttribute(GatewayRequestAttributes.MODEL_NAME)
                    : chatModel.requestedModel(),
                chatModel == null ? null : chatModel.model(),
                chatModel == null ? null : chatModel.provider(),
                chatModel == null ? null : chatModel.deployment(),
                chatModel == null ? null : chatModel.region()),
            new GatewayAuditEvent.Execution(
                outcome,
                httpStatus,
                Duration.between(started, completed).toMillis(),
                null,
                0,
                false),
            new GatewayAuditEvent.GuardrailSummary(
                configured,
                decision == null ? List.of() : decision.evaluated(),
                decision == null ? "NOT_EVALUATED" : decision.decision(),
                decision != null && decision.blocked()),
            new GatewayAuditEvent.Usage(null, null, null, null, null),
            error == null
                ? null
                : new GatewayAuditEvent.ErrorInfo(
                    error.getClass().getSimpleName(), null, safeMessage(error), httpStatus >= 500),
            new GatewayAuditEvent.Retention(
                completed.plus(Duration.ofDays(properties.purgeDays()))));
    auditPublisher.publish(event);
  }

  private final class CompletionListener implements AsyncListener {
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final String requestId;

    private CompletionListener(
        HttpServletRequest request, HttpServletResponse response, String requestId) {
      this.request = request;
      this.response = response;
      this.requestId = requestId;
    }

    @Override
    public void onComplete(AsyncEvent event) {
      publishOnce(request, response, requestId, (Instant) request.getAttribute(STARTED));
    }

    @Override
    public void onTimeout(AsyncEvent event) {
      request.setAttribute(
          GatewayRequestAttributes.ERROR, new IllegalStateException("request timed out"));
      publishOnce(request, response, requestId, (Instant) request.getAttribute(STARTED));
    }

    @Override
    public void onError(AsyncEvent event) {
      request.setAttribute(GatewayRequestAttributes.ERROR, event.getThrowable());
      publishOnce(request, response, requestId, (Instant) request.getAttribute(STARTED));
    }

    @Override
    public void onStartAsync(AsyncEvent event) {}
  }
}
