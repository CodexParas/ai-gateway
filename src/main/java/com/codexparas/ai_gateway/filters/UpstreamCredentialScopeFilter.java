package com.codexparas.ai_gateway.filters;

import com.codexparas.ai_gateway.security.UpstreamCredential;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 25)
public class UpstreamCredentialScopeFilter extends OncePerRequestFilter {
  private final ObjectMapper objectMapper;

  public UpstreamCredentialScopeFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    UpstreamCredential credential =
        (UpstreamCredential) request.getAttribute(GatewayRequestAttributes.UPSTREAM_CREDENTIAL);
    String model = (String) request.getAttribute(GatewayRequestAttributes.MODEL_NAME);
    if (credential == null || model == null || model.isBlank()) {
      chain.doFilter(request, response);
      return;
    }
    List<String> allowedModels = credential.allowedModels();
    if (!allowedModels.isEmpty() && !allowedModels.contains(model)) {
      response.setStatus(403);
      response.setContentType("application/json");
      objectMapper.writeValue(response.getWriter(), java.util.Map.of("error", "model_not_allowed"));
      return;
    }
    if (!credential.scopes().isEmpty() && !credential.scopes().contains("inference")) {
      response.setStatus(403);
      response.setContentType("application/json");
      objectMapper.writeValue(response.getWriter(), java.util.Map.of("error", "scope_not_allowed"));
      return;
    }
    chain.doFilter(request, response);
  }
}
