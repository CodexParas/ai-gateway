package com.codexparas.ai_gateway.exception;

import java.util.List;

public class GuardrailViolationException extends RuntimeException {
  private final List<String> violations;

  public GuardrailViolationException(List<String> violations) {
    super("Request blocked by configured guardrails");
    this.violations = List.copyOf(violations);
  }

  public List<String> violations() {
    return violations;
  }
}
