package com.codexparas.ai_gateway.route.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.codexparas.ai_gateway.route.GatewayRoute;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ProviderRequestValidatorTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void rejectsInvalidSharedOptions() throws Exception {
    OpenAiRequestValidator validator = new OpenAiRequestValidator();
    var body = objectMapper.readTree("{\"model\":\"gpt\",\"messages\":[],\"temperature\":3}");

    List<String> errors =
        validator.validate(new GatewayRoute("OPENAI", "chat.completions", false), body);

    assertThat(errors).contains("messages must be a non-empty array");
    assertThat(errors).contains("temperature must be between 0.0 and 2.0");
  }

  @Test
  void requiresAnthropicMaxTokens() throws Exception {
    AnthropicRequestValidator validator = new AnthropicRequestValidator();
    var body = objectMapper.readTree("{\"model\":\"claude\",\"messages\":[{}]}");

    List<String> errors =
        validator.validate(new GatewayRoute("ANTHROPIC", "messages", false), body);

    assertThat(errors).contains("max_tokens is required for Anthropic messages");
  }
}
