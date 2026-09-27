package com.codexparas.ai_gateway.filters;

import com.codexparas.ai_gateway.exception.GuardrailViolationException;
import com.codexparas.ai_gateway.guardrail.GuardrailDecision;
import com.codexparas.ai_gateway.guardrail.GuardrailFactory;
import com.codexparas.ai_gateway.guardrail.GuardrailResult;
import com.codexparas.ai_gateway.model.PreparedChatModel;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import reactor.core.publisher.Flux;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 55)
public class ResponseGuardrailFilter extends OncePerRequestFilter {
  private final GuardrailFactory guardrailFactory;
  private final ObjectMapper objectMapper;

  public ResponseGuardrailFilter(GuardrailFactory guardrailFactory, ObjectMapper objectMapper) {
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

    ChatResponse chatResponse =
        (ChatResponse) request.getAttribute(GatewayRequestAttributes.CHAT_RESPONSE);
    if (chatResponse != null) {
      try {
        request.setAttribute(
            GatewayRequestAttributes.CHAT_RESPONSE, inspect(request, prepared, chatResponse));
        chain.doFilter(request, response);
      } catch (GuardrailViolationException exception) {
        writeBlockedResponse(response, exception.violations());
      }
      return;
    }

    @SuppressWarnings("unchecked")
    Flux<ChatResponse> stream =
        (Flux<ChatResponse>) request.getAttribute(GatewayRequestAttributes.CHAT_RESPONSE_STREAM);
    if (stream == null) {
      chain.doFilter(request, response);
      return;
    }

    request.setAttribute(
        GatewayRequestAttributes.CHAT_RESPONSE_STREAM,
        stream.map(responseChunk -> inspect(request, prepared, responseChunk)));
    chain.doFilter(request, response);
  }

  private ChatResponse inspect(
      HttpServletRequest request, PreparedChatModel prepared, ChatResponse response) {
    String originalBody = responseText(response);
    String transformedBody = originalBody;
    List<String> configured = new ArrayList<>();
    List<String> evaluated = new ArrayList<>();
    List<String> violations = new ArrayList<>();

    for (JsonNode definition : responseGuardrails(prepared.guardrails())) {
      String id = definition.path("id").asString();
      String type = definition.path("type").asString().toUpperCase(Locale.ROOT);
      configured.add(id);
      evaluated.add(id);
      GuardrailResult result = guardrailFactory.get(type).inspect(definition, transformedBody);
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
    request.setAttribute(GatewayRequestAttributes.RESPONSE_GUARDRAIL_DECISION, decision);
    if (blocked) throw new GuardrailViolationException(violations);
    return Objects.equals(originalBody, transformedBody)
        ? response
        : replaceResponseText(response, transformedBody);
  }

  private static ChatResponse replaceResponseText(ChatResponse response, String text) {
    if (response.getResults().isEmpty()) return response;
    Generation original = response.getResults().getFirst();
    AssistantMessage assistant = original.getOutput();
    AssistantMessage replacement =
        AssistantMessage.builder()
            .content(text)
            .properties(assistant.getMetadata())
            .toolCalls(assistant.getToolCalls())
            .media(assistant.getMedia())
            .build();
    List<Generation> generations = new ArrayList<>(response.getResults());
    generations.set(0, new Generation(replacement, original.getMetadata()));
    return new ChatResponse(generations, response.getMetadata());
  }

  private static String responseText(ChatResponse response) {
    if (response.getResults().isEmpty() || response.getResult().getOutput() == null) return "";
    return response.getResult().getOutput().getText();
  }

  private static List<JsonNode> responseGuardrails(JsonNode guardrails) {
    if (guardrails == null || !guardrails.isArray()) return List.of();
    List<JsonNode> result = new ArrayList<>();
    guardrails.forEach(
        definition -> {
          String stage = definition.path("stage").asString("BOTH");
          if (stage.equalsIgnoreCase("RESPONSE") || stage.equalsIgnoreCase("BOTH")) {
            result.add(definition);
          }
        });
    return result;
  }

  private void writeBlockedResponse(HttpServletResponse response, List<String> violations)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNPROCESSABLE_CONTENT);
    response.setContentType("application/json");
    objectMapper.writeValue(
        response.getWriter(),
        java.util.Map.of(
            "error", "response_guardrail_blocked",
            "message", "Response blocked by configured guardrails",
            "violations", violations));
  }
}
