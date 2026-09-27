package com.codexparas.ai_gateway.guardrail.impl;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class PromptInjectionGuardrail extends PatternGuardrail {
  private static final Pattern INJECTION =
      Pattern.compile(
          "(?is)(ignore|disregard|forget)\\W{0,20}(all\\W+)?(previous|earlier|system|developer)\\W{0,30}(instructions|rules|prompt)|do\\W+anything\\W+now|jailbreak");

  @Override
  public Set<String> supportedTypes() {
    return Set.of("PROMPT_INJECTION", "JAILBREAK");
  }

  @Override
  protected List<Pattern> patterns(JsonNode definition) {
    return List.of(INJECTION);
  }
}
