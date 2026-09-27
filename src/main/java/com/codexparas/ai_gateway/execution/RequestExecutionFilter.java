package com.codexparas.ai_gateway.execution;

import com.codexparas.ai_gateway.config.GatewayExecutionProperties;
import com.codexparas.ai_gateway.filters.GatewayRequestAttributes;
import com.codexparas.ai_gateway.model.PreparedChatModel;
import com.codexparas.ai_gateway.route.GatewayRoute;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import reactor.core.publisher.Flux;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class RequestExecutionFilter extends OncePerRequestFilter {
  private final ObjectMapper objectMapper;
  private final GatewayExecutionProperties executionProperties;
  private final Map<String, RequestTransformer> transformers;

  public RequestExecutionFilter(
      ObjectMapper objectMapper,
      List<RequestTransformer> transformers,
      GatewayExecutionProperties executionProperties) {
    this.objectMapper = objectMapper;
    this.executionProperties = executionProperties;
    this.transformers =
        transformers.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    transformer -> transformer.provider().toUpperCase(Locale.ROOT),
                    Function.identity()));
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    GatewayRoute route = (GatewayRoute) request.getAttribute(GatewayRequestAttributes.ROUTE);
    PreparedChatModel prepared =
        (PreparedChatModel) request.getAttribute(GatewayRequestAttributes.PREPARED_CHAT_MODEL);
    String body = (String) request.getAttribute(GatewayRequestAttributes.REQUEST_BODY);
    if (route == null || prepared == null || body == null) {
      chain.doFilter(request, response);
      return;
    }

    JsonNode requestBody;
    try {
      requestBody = objectMapper.readTree(body);
    } catch (Exception exception) {
      writeError(response, "invalid_json", "Request body is not valid JSON");
      return;
    }

    RequestTransformer transformer = transformers.get(route.provider().toUpperCase(Locale.ROOT));
    if (transformer == null || !transformer.supports(route)) {
      writeError(response, "transformer_not_found", "No request transformer is configured");
      return;
    }

    try {
      JsonNode effectiveRequest = mergeConfiguredDefaults(requestBody, prepared.request());
      TransformedChatRequest transformed = transformer.transform(route, effectiveRequest);
      request.setAttribute(GatewayRequestAttributes.TRANSFORMED_REQUEST, transformed);
      Prompt prompt = new Prompt(transformed.messages(), transformed.chatOptions());
      boolean streaming = route.streaming() || effectiveRequest.path("stream").asBoolean(false);
      if (streaming) {
        Flux<ChatResponse> stream = prepared.chatModel().stream(prompt);
        request.setAttribute(GatewayRequestAttributes.CHAT_RESPONSE_STREAM, stream);
      } else {
        ChatResponse responseBody = executeWithRetry(request, prepared, prompt);
        request.setAttribute(GatewayRequestAttributes.CHAT_RESPONSE, responseBody);
      }
      chain.doFilter(request, response);
    } catch (RuntimeException exception) {
      ProviderExecutionException providerException =
          new ProviderExecutionException(prepared.provider(), prepared.model(), exception);
      request.setAttribute(GatewayRequestAttributes.ERROR, providerException);
      throw providerException;
    }
  }

  private ChatResponse executeWithRetry(
      HttpServletRequest request, PreparedChatModel prepared, Prompt prompt) {
    RuntimeException failure = null;
    for (int attempt = 1; attempt <= executionProperties.maxAttempts(); attempt++) {
      try {
        request.setAttribute(GatewayRequestAttributes.RETRY_COUNT, attempt - 1);
        return prepared.chatModel().call(prompt);
      } catch (RuntimeException exception) {
        failure = exception;
        if (attempt == executionProperties.maxAttempts()) break;
      }
    }
    throw failure == null
        ? new IllegalStateException("Provider execution did not produce a response")
        : failure;
  }

  private static JsonNode mergeConfiguredDefaults(
      JsonNode requestBody, JsonNode configuredRequest) {
    if (configuredRequest == null || !configuredRequest.isObject()) return requestBody;
    ObjectNode merged = (ObjectNode) requestBody.deepCopy();
    configuredRequest
        .properties()
        .forEach(
            entry -> {
              if (!merged.has(entry.getKey())) {
                merged.set(entry.getKey(), entry.getValue().deepCopy());
              }
            });
    return merged;
  }

  private void writeError(HttpServletResponse response, String error, String message)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.setContentType("application/json");
    objectMapper.writeValue(response.getWriter(), Map.of("error", error, "message", message));
  }
}
