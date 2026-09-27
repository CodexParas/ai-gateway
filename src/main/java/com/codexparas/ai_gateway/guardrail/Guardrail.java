package com.codexparas.ai_gateway.guardrail;

import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public interface Guardrail {
  Set<String> supportedTypes();

  GuardrailResult inspect(JsonNode definition, String body);

  default List<String> validate(JsonNode definition, String path) {
    return GuardrailConfigValidation.common(definition, path);
  }
}
