package com.codexparas.ai_gateway.guardrail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class GuardrailFactory {
  private final Map<String, Guardrail> guardrails;

  public GuardrailFactory(List<Guardrail> implementations) {
    Map<String, Guardrail> index = new HashMap<>();
    implementations.forEach(
        implementation ->
            implementation
                .supportedTypes()
                .forEach(type -> index.put(type.toUpperCase(Locale.ROOT), implementation)));
    this.guardrails = Map.copyOf(index);
  }

  public Guardrail get(String type) {
    Guardrail guardrail = guardrails.get(type.toUpperCase(Locale.ROOT));
    if (guardrail == null) {
      throw new IllegalArgumentException("No guardrail implementation for type: " + type);
    }
    return guardrail;
  }

  public List<String> validateAll(JsonNode definitions) {
    if (definitions == null || !definitions.isArray()) {
      return List.of("guardrails must be an array");
    }
    List<String> errors = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    for (int index = 0; index < definitions.size(); index++) {
      String path = "guardrails[" + index + "]";
      JsonNode definition = definitions.get(index);
      if (definition == null || !definition.isObject()) {
        errors.add(path + " must be an object");
        continue;
      }
      String id = definition.path("id").asString();
      if (!id.isBlank() && !ids.add(id)) errors.add(path + ".id must be unique");
      String type = definition.path("type").asString();
      if (type.isBlank()) {
        errors.add(path + ".type must be a non-blank string");
        continue;
      }
      try {
        errors.addAll(get(type).validate(definition, path));
      } catch (IllegalArgumentException exception) {
        errors.add(path + ".type has no registered implementation: " + type);
      }
    }
    return errors;
  }
}
