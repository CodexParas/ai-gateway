package com.codexparas.ai_gateway.filters;

import com.codexparas.ai_gateway.model.ModelConfigurationService;
import com.codexparas.ai_gateway.route.GatewayRoute;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ModelResolutionFilter extends OncePerRequestFilter {
  private static final Pattern PATH_MODEL = Pattern.compile("/models/([^/:]+)(?::[^/]*)?(?:/|$)");

  private final ModelConfigurationService modelConfigurationService;
  private final ObjectMapper objectMapper;

  public ModelResolutionFilter(
      ModelConfigurationService modelConfigurationService, ObjectMapper objectMapper) {
    this.modelConfigurationService = modelConfigurationService;
    this.objectMapper = objectMapper;
  }

  private static String resolveFromPath(HttpServletRequest request) {
    Matcher matcher = PATH_MODEL.matcher(request.getRequestURI());
    return matcher.find() ? URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8) : null;
  }

  private static boolean requiresModel(GatewayRoute route) {
    return !route.operation().equals("models");
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    GatewayRoute route = (GatewayRoute) request.getAttribute(GatewayRequestAttributes.ROUTE);
    if (route == null || !requiresModel(route)) {
      chain.doFilter(request, response);
      return;
    }

    String routeModel = resolveFromPath(request);
    String requestModel = resolveFromBody(request);
    if (routeModel != null && requestModel != null && !routeModel.equals(requestModel)) {
      writeError(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          "model_mismatch",
          "The path model and request body model must match",
          List.of(routeModel, requestModel));
      return;
    }
    String model = routeModel != null ? routeModel : requestModel;
    if (model == null) model = request.getParameter("model");
    if (model == null || model.isBlank()) {
      writeError(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          "model_required",
          "A model is required for this operation",
          List.of());
      return;
    }

    request.setAttribute(GatewayRequestAttributes.MODEL_NAME, model);
    try {
      JsonNode configuration =
          modelConfigurationService.findAndValidate(model).block(Duration.ofSeconds(5));
      request.setAttribute(GatewayRequestAttributes.MODEL_CONFIGURATION, configuration);
      chain.doFilter(request, response);
    } catch (ModelConfigurationService.ModelConfigurationNotFoundException exception) {
      writeError(
          response,
          HttpServletResponse.SC_NOT_FOUND,
          "model_configuration_not_found",
          "No configuration exists for the requested model",
          List.of(model));
    } catch (ModelConfigurationService.ModelConfigurationInvalidException exception) {
      writeError(
          response,
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "model_configuration_invalid",
          "The model configuration failed validation",
          exception.errors());
    } catch (ModelConfigurationService.ModelConfigurationLookupException exception) {
      writeError(
          response,
          HttpServletResponse.SC_SERVICE_UNAVAILABLE,
          "model_configuration_unavailable",
          "The model configuration store could not be reached",
          List.of(model));
    }
  }

  private String resolveFromBody(HttpServletRequest request) throws IOException {
    String body = (String) request.getAttribute(GatewayRequestAttributes.REQUEST_BODY);
    if (body == null || body.isBlank()) return null;
    try {
      JsonNode json = objectMapper.readTree(body);
      JsonNode model = json.path("model");
      return model.isString() ? model.asString() : null;
    } catch (Exception exception) {
      return null;
    }
  }

  private void writeError(
      HttpServletResponse response, int status, String error, String message, List<String> details)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getWriter(), Map.of("error", error, "message", message, "details", details));
  }
}
