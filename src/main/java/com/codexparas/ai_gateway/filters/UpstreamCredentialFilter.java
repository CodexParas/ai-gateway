package com.codexparas.ai_gateway.filters;

import com.codexparas.ai_gateway.config.GatewaySecurityProperties;
import com.codexparas.ai_gateway.security.UpstreamCredential;
import com.codexparas.ai_gateway.security.UpstreamCredentialService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class UpstreamCredentialFilter extends OncePerRequestFilter {
  private final UpstreamCredentialService credentialService;
  private final GatewaySecurityProperties properties;
  private final ObjectMapper objectMapper;

  public UpstreamCredentialFilter(
      UpstreamCredentialService credentialService,
      GatewaySecurityProperties properties,
      ObjectMapper objectMapper) {
    this.credentialService = credentialService;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    if (!properties.enabled() || request.getRequestURI().startsWith("/actuator/")) {
      chain.doFilter(request, response);
      return;
    }
    String token = extractToken(request);
    if (token == null) {
      writeError(response, 401, "missing_credentials");
      return;
    }
    try {
      UpstreamCredential credential = credentialService.resolve(token).block(Duration.ofSeconds(5));
      request.setAttribute(GatewayRequestAttributes.UPSTREAM_CREDENTIAL, credential);
      chain.doFilter(request, response);
    } catch (UpstreamCredentialService.CredentialRejectedException exception) {
      writeError(response, 401, exception.getMessage());
    } catch (UpstreamCredentialService.CredentialStoreException exception) {
      writeError(response, 503, "credential_store_unavailable");
    }
  }

  private static String extractToken(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization != null) {
      String[] parts = authorization.trim().split("\\s+", 2);
      if (parts.length == 2
          && (parts[0].equalsIgnoreCase("Bearer") || parts[0].equalsIgnoreCase("ApiKey"))) {
        return parts[1];
      }
    }
    String apiKey = request.getHeader("X-API-Key");
    return apiKey == null || apiKey.isBlank() ? null : apiKey;
  }

  private void writeError(HttpServletResponse response, int status, String code)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.getWriter().write(objectMapper.writeValueAsString(java.util.Map.of("error", code)));
  }
}
