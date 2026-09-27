package com.codexparas.ai_gateway.guardrail.impl;

import com.codexparas.ai_gateway.guardrail.Guardrail;
import com.codexparas.ai_gateway.guardrail.GuardrailResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class NoOpGuardrail implements Guardrail {
  @Override
  public Set<String> supportedTypes() {
    return Set.of("JSON_SCHEMA", "CUSTOM");
  }

  @Override
  public GuardrailResult inspect(JsonNode definition, String body) {
    return GuardrailResult.noMatch(body);
  }

  @Override
  public List<String> validate(JsonNode definition, String path) {
    List<String> errors = new ArrayList<>(Guardrail.super.validate(definition, path));
    String type = definition.path("type").asString();
    if (type.equals("JSON_SCHEMA") && !definition.path("config").path("schema").isObject()) {
      errors.add(path + ".config.schema must be an object for JSON_SCHEMA");
    }
    if (type.equals("CUSTOM")
        && (!definition.path("policyRef").isString()
            || definition.path("policyRef").asString().isBlank())) {
      errors.add(path + ".policyRef is required for CUSTOM");
    }
    return errors;
  }
}
