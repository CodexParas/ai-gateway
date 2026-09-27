package com.codexparas.ai_gateway.guardrail;

public record GuardrailResult(boolean matched, String transformedBody) {
  public static GuardrailResult noMatch(String body) {
    return new GuardrailResult(false, body);
  }
}
