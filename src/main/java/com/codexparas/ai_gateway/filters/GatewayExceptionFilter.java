package com.codexparas.ai_gateway.filters;

import com.codexparas.ai_gateway.exception.GuardrailViolationException;
import com.codexparas.ai_gateway.execution.ProviderExecutionException;
import com.codexparas.ai_gateway.model.ChatModelFactory;
import com.codexparas.ai_gateway.model.ModelConfigurationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/** Converts exceptions raised by the Servlet gateway pipeline into safe HTTP errors. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class GatewayExceptionFilter extends OncePerRequestFilter {
  private final ObjectMapper objectMapper;

  public GatewayExceptionFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    try {
      chain.doFilter(request, response);
    } catch (RuntimeException exception) {
      request.setAttribute(GatewayRequestAttributes.ERROR, exception);
      recordProviderFailure(request, exception);
      if (response.isCommitted()) throw exception;
      writeError(response, exception);
    } catch (ServletException exception) {
      request.setAttribute(GatewayRequestAttributes.ERROR, exception);
      recordProviderFailure(request, exception);
      if (response.isCommitted()) throw exception;
      writeError(response, exception);
    } catch (IOException exception) {
      request.setAttribute(GatewayRequestAttributes.ERROR, exception);
      recordProviderFailure(request, exception);
      if (response.isCommitted()) throw exception;
      writeError(response, exception);
    }
  }

  private static void recordProviderFailure(HttpServletRequest request, Exception exception) {
    if (exception instanceof ProviderExecutionException) {
      request.setAttribute(
          GatewayRequestAttributes.PROVIDER_STATUS, HttpServletResponse.SC_BAD_GATEWAY);
      request.setAttribute(
          GatewayRequestAttributes.PROVIDER_ERROR_CODE, "provider_execution_failed");
    }
  }

  private void writeError(HttpServletResponse response, Exception exception) throws IOException {
    Throwable cause = rootCause(exception);
    ErrorDetails details = details(cause);
    response.resetBuffer();
    response.setStatus(details.status());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getWriter(),
        Map.of(
            "error", details.code(),
            "message", details.message(),
            "details", details.details()));
  }

  private static ErrorDetails details(Throwable exception) {
    if (exception instanceof GuardrailViolationException guardrail) {
      return new ErrorDetails(
          HttpServletResponse.SC_UNPROCESSABLE_CONTENT,
          "guardrail_blocked",
          "Request blocked by configured guardrails",
          guardrail.violations());
    }
    if (exception instanceof ModelConfigurationService.ModelConfigurationNotFoundException) {
      return new ErrorDetails(
          HttpServletResponse.SC_NOT_FOUND,
          "model_configuration_not_found",
          "No configuration exists for the requested model",
          List.of());
    }
    if (exception instanceof ModelConfigurationService.ModelConfigurationInvalidException invalid) {
      return new ErrorDetails(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "model_configuration_invalid",
          "The model configuration failed validation",
          invalid.errors());
    }
    if (exception instanceof ModelConfigurationService.ModelConfigurationLookupException) {
      return new ErrorDetails(
          HttpServletResponse.SC_SERVICE_UNAVAILABLE,
          "model_configuration_unavailable",
          "The model configuration store could not be reached",
          List.of());
    }
    if (exception instanceof ChatModelFactory.ChatModelPreparationException) {
      return new ErrorDetails(
          HttpServletResponse.SC_SERVICE_UNAVAILABLE,
          "chat_model_unavailable",
          "The requested provider model could not be prepared",
          List.of());
    }
    if (exception instanceof ProviderExecutionException) {
      return new ErrorDetails(
          HttpServletResponse.SC_BAD_GATEWAY,
          "provider_execution_failed",
          "The upstream provider request failed",
          List.of());
    }
    if (exception instanceof IllegalArgumentException) {
      return new ErrorDetails(
          HttpServletResponse.SC_BAD_REQUEST,
          "bad_request",
          "The gateway could not process the request",
          List.of());
    }
    return new ErrorDetails(
        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
        "gateway_error",
        "The gateway could not process the request",
        List.of());
  }

  private static Throwable rootCause(Throwable exception) {
    Throwable current = exception;
    while (current.getCause() != null
        && (current instanceof java.util.concurrent.CompletionException
            || current instanceof java.util.concurrent.ExecutionException
            || current instanceof ServletException)) {
      current = current.getCause();
    }
    return current;
  }

  private record ErrorDetails(int status, String code, String message, List<String> details) {}
}
