package com.codexparas.ai_gateway.guardrail;

import java.util.List;

public record GuardrailDecision(
    List<String> configured,
    List<String> evaluated,
    List<String> violations,
    String decision,
    boolean blocked,
    String transformedBody) {}
