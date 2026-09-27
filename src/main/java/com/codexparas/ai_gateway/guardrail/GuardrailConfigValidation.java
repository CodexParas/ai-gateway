package com.codexparas.ai_gateway.guardrail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;

final class GuardrailConfigValidation {
  private static final Set<String> STAGES = Set.of("REQUEST", "RESPONSE", "BOTH");
  private static final Set<String> ACTIONS = Set.of("BLOCK", "REDACT", "WARN", "TRANSFORM");

  private GuardrailConfigValidation() {}

  static List<String> common(JsonNode definition, String path) {
    List<String> errors = new ArrayList<>();
    if (definition == null || !definition.isObject()) {
      return List.of(path + " must be an object");
    }
    requiredText(definition, "id", path, errors);
    requiredText(definition, "type", path, errors);
    requiredText(definition, "stage", path, errors);
    requiredText(definition, "action", path, errors);
    if (definition.path("stage").isString()
        && !STAGES.contains(definition.path("stage").asString().toUpperCase())) {
      errors.add(path + ".stage must be REQUEST, RESPONSE, or BOTH");
    }
    if (definition.path("action").isString()
        && !ACTIONS.contains(definition.path("action").asString().toUpperCase())) {
      errors.add(path + ".action must be BLOCK, REDACT, WARN, or TRANSFORM");
    }
    return errors;
  }

  static void requiredText(JsonNode object, String field, String path, List<String> errors) {
    if (!object.has(field)
        || !object.get(field).isString()
        || object.get(field).asString().isBlank()) {
      errors.add(path + "." + field + " must be a non-blank string");
    }
  }
}
