package com.codexparas.ai_gateway.execution;

import static org.assertj.core.api.Assertions.assertThat;

import com.codexparas.ai_gateway.route.GatewayRoute;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import tools.jackson.databind.ObjectMapper;

class ResponseTransformerTest {

  @Test
  void transformsSpringAiResponseToOpenAiShape() throws Exception {
    OpenAiResponseTransformer transformer = new OpenAiResponseTransformer(new ObjectMapper());
    ChatResponse response =
        new ChatResponse(List.of(new Generation(new AssistantMessage("Hello"))));

    ProviderResponse result =
        transformer.transform(new GatewayRoute("OPENAI", "chat.completions", false), response);

    assertThat(result.body().path("object").asString()).isEqualTo("chat.completion");
    assertThat(result.body().path("choices").get(0).path("message").path("content").asString())
        .isEqualTo("Hello");
  }
}
