package com.codexparas.ai_gateway.route;

import com.codexparas.ai_gateway.filters.GatewayRequestAttributes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/** Resolves the canonical /{provider}/{operation} gateway route. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 8)
public class GatewayRouteFilter extends OncePerRequestFilter {
  private static final Set<String> PROVIDERS =
      Set.of("openai", "anthropic", "bedrock", "google", "azure");

  private final ObjectMapper objectMapper;

  public GatewayRouteFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    if (isInfrastructurePath(request)) {
      chain.doFilter(request, response);
      return;
    }

    GatewayRoute route = resolve(request);
    if (route == null) {
      if (isProviderPath(request)) {
        writeUnsupportedRoute(response, request);
        return;
      }
      chain.doFilter(request, response);
      return;
    }

    request.setAttribute(GatewayRequestAttributes.ROUTE, route);
    chain.doFilter(request, response);
  }

  private static GatewayRoute resolve(HttpServletRequest request) {
    String path = normalize(request.getRequestURI());
    String[] segments = path.substring(1).split("/", 2);
    if (segments.length < 2 || !PROVIDERS.contains(segments[0])) return null;

    String provider = segments[0].toUpperCase(Locale.ROOT);
    String operationPath = segments[1];
    String operation = operation(provider, operationPath);
    if (operation == null) return null;

    return new GatewayRoute(provider, operation, isStreaming(request, path));
  }

  private static String operation(String provider, String path) {
    return switch (provider) {
      case "OPENAI" -> openAiOperation(path);
      case "ANTHROPIC" ->
          path.equals("messages") || path.equals("messages/stream") ? "messages" : null;
      case "BEDROCK" -> bedrockOperation(path);
      case "GOOGLE" -> googleOperation(path);
      case "AZURE" -> azureOperation(path);
      default -> null;
    };
  }

  private static String openAiOperation(String path) {
    if (path.equals("chat/completions")) return "chat.completions";
    if (path.equals("responses") || path.equals("responses/stream")) return "responses";
    if (path.equals("embeddings")) return "embeddings";
    if (path.equals("models") || path.matches("models/[^/]+")) return "models";
    if (path.equals("audio/speech")) return "audio.speech";
    if (path.equals("audio/transcriptions")) return "audio.transcriptions";
    return null;
  }

  private static String bedrockOperation(String path) {
    if (path.equals("models") || path.matches("models/[^/]+")) return "models";
    return path.matches("models/[^/]+/converse") ? "converse" : null;
  }

  private static String googleOperation(String path) {
    if (path.equals("models")) return "models";
    return path.matches("models/[^/:]+:generateContent") ? "generate.content" : null;
  }

  private static String azureOperation(String path) {
    if (path.equals("chat/completions")) return "chat.completions";
    if (path.equals("embeddings")) return "embeddings";
    return null;
  }

  private static boolean isStreaming(HttpServletRequest request, String path) {
    return path.endsWith("/stream")
        || "true".equalsIgnoreCase(request.getParameter("stream"))
        || acceptsEventStream(request);
  }

  private static boolean acceptsEventStream(HttpServletRequest request) {
    String accept = request.getHeader("Accept");
    return accept != null
        && accept.toLowerCase(Locale.ROOT).contains(MediaType.TEXT_EVENT_STREAM_VALUE);
  }

  private static String normalize(String uri) {
    if (uri == null || uri.isBlank()) return "/";
    String path = uri.length() > 1 && uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    return path.toLowerCase(Locale.ROOT);
  }

  private static boolean isProviderPath(HttpServletRequest request) {
    String path = normalize(request.getRequestURI());
    if (!path.startsWith("/") || path.length() < 2) return false;
    int separator = path.indexOf('/', 1);
    String provider = separator < 0 ? path.substring(1) : path.substring(1, separator);
    return PROVIDERS.contains(provider);
  }

  private static boolean isInfrastructurePath(HttpServletRequest request) {
    String path = normalize(request.getRequestURI());
    return path.equals("/actuator") || path.startsWith("/actuator/") || path.equals("/favicon.ico");
  }

  private void writeUnsupportedRoute(HttpServletResponse response, HttpServletRequest request)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getWriter(),
        java.util.Map.of(
            "error", "unsupported_route",
            "message", "No gateway route matches the request",
            "path", request.getRequestURI()));
  }
}
