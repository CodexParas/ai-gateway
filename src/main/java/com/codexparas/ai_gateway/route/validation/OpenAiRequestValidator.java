package com.codexparas.ai_gateway.route.validation;

import com.codexparas.ai_gateway.route.GatewayRoute;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public final class OpenAiRequestValidator extends AbstractProviderRequestValidator {
  @Override
  public String provider() {
    return "OPENAI";
  }

  @Override
  protected void validateProviderOptions(GatewayRoute route, JsonNode body, List<String> errors) {
    JsonNode responseFormat = body.get("response_format");
    if (responseFormat != null && !responseFormat.isObject()) {
      errors.add("response_format must be an object");
    }
    JsonNode tools = body.get("tools");
    if (tools != null && !tools.isArray()) errors.add("tools must be an array");
  }
}
