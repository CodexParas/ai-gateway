package com.codexparas.ai_gateway.guardrail.impl;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class DlpGuardrail extends PatternGuardrail {
  private static final Pattern JWT =
      Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
  private static final Pattern AWS_ACCESS_KEY = Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b");
  private static final Pattern CREDIT_CARD = Pattern.compile("(?<!\\d)(?:\\d[ -]*?){13,19}(?!\\d)");

  @Override
  public Set<String> supportedTypes() {
    return Set.of("DLP");
  }

  @Override
  protected List<Pattern> patterns(JsonNode definition) {
    return List.of(JWT, AWS_ACCESS_KEY, CREDIT_CARD);
  }
}
