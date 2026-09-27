package com.codexparas.ai_gateway.gateway;

import com.codexparas.ai_gateway.filters.GatewayRequestAttributes;
import com.codexparas.ai_gateway.route.GatewayRoute;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

/** Terminal servlet for canonical provider paths after the gateway filters have run. */
public final class GatewayFallbackServlet extends HttpServlet {
  private final ObjectMapper objectMapper;

  public GatewayFallbackServlet(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    GatewayRoute route = (GatewayRoute) request.getAttribute(GatewayRequestAttributes.ROUTE);
    if (route == null) {
      write(
          response,
          HttpServletResponse.SC_NOT_FOUND,
          "gateway_route_not_found",
          "No gateway route matched the request");
      return;
    }
    write(
        response,
        HttpServletResponse.SC_NOT_IMPLEMENTED,
        "gateway_operation_not_completed",
        "The gateway pipeline did not produce a provider response");
  }

  private void write(HttpServletResponse response, int status, String error, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    objectMapper.writeValue(response.getWriter(), Map.of("error", error, "message", message));
  }
}
