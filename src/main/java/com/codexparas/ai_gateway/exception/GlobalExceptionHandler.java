package com.codexparas.ai_gateway.exception;

import com.codexparas.ai_gateway.dto.ValidationErrorResponse;
import com.codexparas.ai_gateway.execution.ProviderExecutionException;
import com.codexparas.ai_gateway.model.ModelConfigurationService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(WebExchangeBindException.class)
  ResponseEntity<ValidationErrorResponse> handleValidation(WebExchangeBindException exception) {
    List<ValidationErrorResponse.FieldError> fields =
        exception.getFieldErrors().stream()
            .map(
                error ->
                    new ValidationErrorResponse.FieldError(
                        error.getField(), error.getDefaultMessage()))
            .toList();
    return ResponseEntity.badRequest()
        .body(new ValidationErrorResponse("validation_failed", fields));
  }

  @ExceptionHandler(GuardrailViolationException.class)
  ResponseEntity<Map<String, Object>> handleGuardrailViolation(
      GuardrailViolationException exception) {
    return ResponseEntity.unprocessableContent()
        .body(
            Map.of(
                "error", "guardrail_blocked",
                "message", exception.getMessage(),
                "violations", exception.violations()));
  }

  @ExceptionHandler(ModelConfigurationService.ModelConfigurationNotFoundException.class)
  ResponseEntity<Map<String, Object>> handleModelConfigurationNotFound(
      ModelConfigurationService.ModelConfigurationNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "error", "model_configuration_not_found",
                "model", exception.model(),
                "message", "No configuration exists for the requested model"));
  }

  @ExceptionHandler(ModelConfigurationService.ModelConfigurationInvalidException.class)
  ResponseEntity<Map<String, Object>> handleInvalidModelConfiguration(
      ModelConfigurationService.ModelConfigurationInvalidException exception) {
    return ResponseEntity.internalServerError()
        .body(
            Map.of(
                "error",
                "model_configuration_invalid",
                "model",
                exception.model(),
                "message",
                "The model configuration failed validation",
                "details",
                exception.errors()));
  }

  @ExceptionHandler(ModelConfigurationService.ModelConfigurationLookupException.class)
  ResponseEntity<Map<String, String>> handleModelConfigurationLookup(
      ModelConfigurationService.ModelConfigurationLookupException exception) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(
            Map.of(
                "error", "model_configuration_unavailable",
                "model", exception.model(),
                "message", "The model configuration store could not be reached"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
    return ResponseEntity.internalServerError()
        .body(Map.of("error", "inference_failed", "message", "Inference request failed"));
  }

  @ExceptionHandler(ProviderExecutionException.class)
  ResponseEntity<Map<String, String>> handleProviderExecution(
      ProviderExecutionException exception) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(
            Map.of(
                "error", "provider_execution_failed",
                "message", "The upstream provider request failed"));
  }
}
