package com.codexparas.ai_gateway.filters;

import com.codexparas.ai_gateway.guardrail.Guardrail;
import com.codexparas.ai_gateway.guardrail.GuardrailDecision;
import com.codexparas.ai_gateway.guardrail.GuardrailFactory;
import com.codexparas.ai_gateway.guardrail.GuardrailResult;
import com.codexparas.ai_gateway.model.PreparedChatModel;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Order(Ordered.HIGHEST_PRECEDENCE + 40)
@Component
public class GuardrailFilter extends OncePerRequestFilter {
  private final GuardrailFactory guardrailFactory;
  private final ObjectMapper objectMapper;

  public GuardrailFilter(GuardrailFactory guardrailFactory, ObjectMapper objectMapper) {
    this.guardrailFactory = guardrailFactory;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    PreparedChatModel prepared =
        (PreparedChatModel) request.getAttribute(GatewayRequestAttributes.PREPARED_CHAT_MODEL);
    if (prepared == null) {
      chain.doFilter(request, response);
      return;
    }
    String originalBody = (String) request.getAttribute(GatewayRequestAttributes.REQUEST_BODY);
    String transformedBody = originalBody;
    List<String> configured = new ArrayList<>();
    List<String> evaluated = new ArrayList<>();
    List<String> violations = new ArrayList<>();
    for (JsonNode definition : requestGuardrails(prepared.guardrails())) {
      String id = definition.path("id").asString();
      String type = definition.path("type").asString().toUpperCase(Locale.ROOT);
      configured.add(id);
      Guardrail guardrail = guardrailFactory.get(type);
      evaluated.add(id);
      GuardrailResult result = guardrail.inspect(definition, transformedBody);
      transformedBody = result.transformedBody();
      if (result.matched()
          && definition.path("action").asString("BLOCK").equalsIgnoreCase("BLOCK")) {
        violations.add(id);
      }
    }
    boolean blocked = !violations.isEmpty();
    GuardrailDecision decision =
        new GuardrailDecision(
            configured,
            evaluated,
            violations,
            blocked ? "BLOCK" : Objects.equals(originalBody, transformedBody) ? "ALLOW" : "REDACT",
            blocked,
            transformedBody);
    request.setAttribute(GatewayRequestAttributes.GUARDRAIL_DECISION, decision);
    if (blocked) {
      writeBlockedResponse(response, violations);
      return;
    }
    if (Objects.equals(originalBody, transformedBody)) {
      chain.doFilter(request, response);
      return;
    }
    request.setAttribute(GatewayRequestAttributes.REQUEST_BODY, transformedBody);
    chain.doFilter(
        new CachedBodyHttpServletRequest(
            request,
            transformedBody == null
                ? new byte[0]
                : transformedBody.getBytes(StandardCharsets.UTF_8)),
        response);
  }

  private void writeBlockedResponse(HttpServletResponse response, List<String> violations)
      throws IOException {
    response.setStatus(422);
    response.setContentType("application/json");
    objectMapper.writeValue(
        response.getWriter(),
        java.util.Map.of(
            "error", "guardrail_blocked",
            "message", "Request blocked by configured guardrails",
            "violations", violations));
  }

  private static List<JsonNode> requestGuardrails(JsonNode guardrails) {
    if (guardrails == null || !guardrails.isArray()) return List.of();
    List<JsonNode> result = new ArrayList<>();
    guardrails.forEach(
        definition -> {
          String stage = definition.path("stage").asString("BOTH");
          if (stage.equalsIgnoreCase("REQUEST") || stage.equalsIgnoreCase("BOTH")) {
            result.add(definition);
          }
        });
    return result;
  }
}
