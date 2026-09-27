package com.codexparas.ai_gateway.route.validation;

import com.codexparas.ai_gateway.route.GatewayRoute;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public final class AnthropicRequestValidator extends AbstractProviderRequestValidator {
  @Override
  public String provider() {
    return "ANTHROPIC";
  }

  @Override
  protected void validateProviderOptions(GatewayRoute route, JsonNode body, List<String> errors) {
    if ("messages".equals(route.operation())) {
      JsonNode maxTokens = body.get("max_tokens");
      if (maxTokens == null) errors.add("max_tokens is required for Anthropic messages");
    }
    JsonNode tools = body.get("tools");
    if (tools != null && !tools.isArray()) errors.add("tools must be an array");
  }
}
