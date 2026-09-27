package com.codexparas.ai_gateway.route.validation;

import com.codexparas.ai_gateway.filters.CachedBodyHttpServletRequest;
import com.codexparas.ai_gateway.filters.GatewayRequestAttributes;
import com.codexparas.ai_gateway.route.GatewayRoute;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 12)
public class ProviderRequestValidationFilter extends OncePerRequestFilter {
  private final ObjectMapper objectMapper;
  private final Map<String, ProviderRequestValidator> validators;

  public ProviderRequestValidationFilter(
      ObjectMapper objectMapper, List<ProviderRequestValidator> validators) {
    this.objectMapper = objectMapper;
    this.validators =
        validators.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    validator -> validator.provider().toUpperCase(Locale.ROOT),
                    Function.identity()));
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    GatewayRoute route = (GatewayRoute) request.getAttribute(GatewayRequestAttributes.ROUTE);
    if (route == null || !requiresJsonBody(route)) {
      chain.doFilter(request, response);
      return;
    }

    HttpServletRequest requestToContinue = request;
    String body = (String) request.getAttribute(GatewayRequestAttributes.REQUEST_BODY);
    if (body == null && isJsonBody(request)) {
      byte[] bytes = request.getInputStream().readAllBytes();
      body = new String(bytes, StandardCharsets.UTF_8);
      request.setAttribute(GatewayRequestAttributes.REQUEST_BODY, body);
      requestToContinue = new CachedBodyHttpServletRequest(request, bytes);
    }

    JsonNode json;
    try {
      json = body == null || body.isBlank() ? null : objectMapper.readTree(body);
    } catch (JacksonException exception) {
      writeError(response, "invalid_json", "Request body is not valid JSON", List.of());
      return;
    }

    ProviderRequestValidator validator = validators.get(route.provider().toUpperCase(Locale.ROOT));
    if (validator == null) {
      writeError(response, "unsupported_provider", "No request validator is configured", List.of());
      return;
    }
    List<String> errors = validator.validate(route, json);
    if (!errors.isEmpty()) {
      writeError(
          response, "request_validation_failed", "Provider request validation failed", errors);
      return;
    }
    chain.doFilter(requestToContinue, response);
  }

  private static boolean requiresJsonBody(GatewayRoute route) {
    return SetOfBodyOperations.VALUE.contains(route.operation());
  }

  private static boolean isJsonBody(HttpServletRequest request) {
    String contentType = request.getContentType();
    return contentType != null
        && MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON);
  }

  private void writeError(
      HttpServletResponse response, String error, String message, List<String> details)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getWriter(), Map.of("error", error, "message", message, "details", details));
  }

  private static final class SetOfBodyOperations {
    private static final java.util.Set<String> VALUE =
        java.util.Set.of(
            "chat.completions",
            "responses",
            "messages",
            "converse",
            "generate.content",
            "embeddings");
  }
}
