package com.codexparas.ai_gateway.route.validation;

import com.codexparas.ai_gateway.route.GatewayRoute;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

abstract class AbstractProviderRequestValidator implements ProviderRequestValidator {

  @Override
  public List<String> validate(GatewayRoute route, JsonNode body) {
    List<String> errors = new ArrayList<>();
    if (body == null || !body.isObject()) {
      return List.of("request body must be a JSON object");
    }

    switch (route.operation()) {
      case "chat.completions", "responses", "messages", "converse" -> {
        requireArray(body, "messages", errors);
        requireText(body, "model", errors, false);
      }
      case "generate.content" -> requireArray(body, "contents", errors);
      case "embeddings" -> {
        requireText(body, "input", errors, false);
        requireText(body, "model", errors, false);
      }
      default -> {
        // Listing and metadata operations do not require a JSON request body.
      }
    }
    validateCommonOptions(route, body, errors);
    validateProviderOptions(route, body, errors);
    return errors;
  }

  protected void validateProviderOptions(GatewayRoute route, JsonNode body, List<String> errors) {}

  private static void validateCommonOptions(
      GatewayRoute route, JsonNode body, List<String> errors) {
    numberInRange(body, "temperature", 0, 2, errors);
    numberInRange(body, "top_p", 0, 1, errors);
    numberInRange(body, "topP", 0, 1, errors);
    positiveInteger(body, "max_tokens", errors);
    positiveInteger(body, "max_completion_tokens", errors);
    positiveInteger(body, "top_k", errors);
    JsonNode stream = body.get("stream");
    if (stream != null && !stream.isBoolean()) errors.add("stream must be boolean");
    JsonNode stop = body.get("stop");
    if (stop != null && !stop.isTextual() && !stop.isArray()) {
      errors.add("stop must be text or an array");
    }
    if (stop != null && stop.isArray()) {
      for (JsonNode value : stop) {
        if (!value.isTextual()) {
          errors.add("stop array values must be text");
          break;
        }
      }
    }
  }

  private static void numberInRange(
      JsonNode body, String field, double minimum, double maximum, List<String> errors) {
    JsonNode value = body.get(field);
    if (value != null
        && (!value.isNumber() || value.asDouble() < minimum || value.asDouble() > maximum)) {
      errors.add(field + " must be between " + minimum + " and " + maximum);
    }
  }

  private static void positiveInteger(JsonNode body, String field, List<String> errors) {
    JsonNode value = body.get(field);
    if (value != null && (!value.isIntegralNumber() || value.asInt() <= 0)) {
      errors.add(field + " must be a positive integer");
    }
  }

  private static void requireArray(JsonNode body, String field, List<String> errors) {
    if (!body.path(field).isArray() || body.path(field).isEmpty()) {
      errors.add(field + " must be a non-empty array");
    }
  }

  private static void requireText(
      JsonNode body, String field, List<String> errors, boolean required) {
    JsonNode value = body.get(field);
    if (value == null || !value.isString() || (required && value.asString().isBlank())) {
      errors.add(field + " must be text");
    }
  }
}
