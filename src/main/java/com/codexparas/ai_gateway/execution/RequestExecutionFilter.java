package com.codexparas.ai_gateway.execution;

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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class RequestExecutionFilter extends OncePerRequestFilter {
  private final ObjectMapper objectMapper;
  private final Map<String, RequestTransformer> transformers;

  public RequestExecutionFilter(ObjectMapper objectMapper, List<RequestTransformer> transformers) {
    this.objectMapper = objectMapper;
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
      TransformedChatRequest transformed = transformer.transform(route, requestBody);
      request.setAttribute(GatewayRequestAttributes.TRANSFORMED_REQUEST, transformed);
      Prompt prompt = new Prompt(transformed.messages(), transformed.chatOptions());
      boolean streaming = route.streaming() || requestBody.path("stream").asBoolean(false);
      if (streaming) {
        Flux<ChatResponse> stream = prepared.chatModel().stream(prompt);
        request.setAttribute(GatewayRequestAttributes.CHAT_RESPONSE_STREAM, stream);
      } else {
        ChatResponse responseBody = prepared.chatModel().call(prompt);
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

  private void writeError(HttpServletResponse response, String error, String message)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.setContentType("application/json");
    objectMapper.writeValue(response.getWriter(), Map.of("error", error, "message", message));
  }
}
