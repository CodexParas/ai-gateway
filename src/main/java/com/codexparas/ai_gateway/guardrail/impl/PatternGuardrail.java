package com.codexparas.ai_gateway.guardrail.impl;

import com.codexparas.ai_gateway.guardrail.Guardrail;
import com.codexparas.ai_gateway.guardrail.GuardrailResult;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

abstract class PatternGuardrail implements Guardrail {
  @Override
  public GuardrailResult inspect(JsonNode definition, String body) {
    if (body == null || body.isBlank()) return GuardrailResult.noMatch(body);
    List<Pattern> patterns = patterns(definition);
    boolean matched = patterns.stream().anyMatch(pattern -> pattern.matcher(body).find());
    if (!matched) return GuardrailResult.noMatch(body);
    String action = definition.path("action").asString("BLOCK");
    if (Set.of("REDACT", "TRANSFORM").contains(action)) {
      String transformed = body;
      for (Pattern pattern : patterns) {
        transformed = pattern.matcher(transformed).replaceAll("[REDACTED]");
      }
      return new GuardrailResult(true, transformed);
    }
    return new GuardrailResult(true, body);
  }

  protected abstract List<Pattern> patterns(JsonNode definition);
}
