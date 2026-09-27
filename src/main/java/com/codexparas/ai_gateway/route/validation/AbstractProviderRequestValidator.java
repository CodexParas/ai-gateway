package com.codexparas.ai_gateway.route.validation;

import com.codexparas.ai_gateway.route.GatewayRoute;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

abstract class AbstractProviderRequestValidator implements ProviderRequestValidator {

  @Override
  public final List<String> validate(GatewayRoute route, JsonNode body) {
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
    return errors;
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
