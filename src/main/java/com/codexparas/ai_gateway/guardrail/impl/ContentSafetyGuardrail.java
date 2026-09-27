package com.codexparas.ai_gateway.guardrail.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class ContentSafetyGuardrail extends PatternGuardrail {
  @Override
  public List<String> validate(JsonNode definition, String path) {
    List<String> errors = new ArrayList<>(super.validate(definition, path));
    JsonNode terms = definition.path("config").path("blockedTerms");
    if (!terms.isArray() || terms.isEmpty()) {
      errors.add(path + ".config.blockedTerms must contain at least one term");
    } else {
      for (int index = 0; index < terms.size(); index++) {
        if (!terms.get(index).isString() || terms.get(index).asString().isBlank()) {
          errors.add(path + ".config.blockedTerms[" + index + "] must be non-blank text");
        }
      }
    }
    return errors;
  }

  @Override
  public Set<String> supportedTypes() {
    return Set.of("TOXICITY", "CONTENT_SAFETY");
  }

  @Override
  protected List<Pattern> patterns(JsonNode definition) {
    List<Pattern> patterns = new ArrayList<>();
    JsonNode terms = definition.path("config").path("blockedTerms");
    if (terms.isArray()) {
      terms.forEach(
          term -> {
            if (term.isString()) {
              patterns.add(
                  Pattern.compile(Pattern.quote(term.asString()), Pattern.CASE_INSENSITIVE));
            }
          });
    }
    return patterns;
  }
}
