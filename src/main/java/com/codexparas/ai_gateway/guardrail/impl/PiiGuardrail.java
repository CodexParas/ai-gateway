package com.codexparas.ai_gateway.guardrail.impl;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class PiiGuardrail extends PatternGuardrail {
  private static final Pattern EMAIL =
      Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
  private static final Pattern PHONE = Pattern.compile("(?<!\\d)\\+?\\d[\\d .()\\-]{7,}\\d(?!\\d)");

  @Override
  public Set<String> supportedTypes() {
    return Set.of("PII_REDACTION");
  }

  @Override
  protected List<Pattern> patterns(JsonNode definition) {
    return List.of(EMAIL, PHONE);
  }
}
