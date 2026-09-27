package com.codexparas.ai_gateway.execution;

import static org.assertj.core.api.Assertions.assertThat;

import com.codexparas.ai_gateway.route.GatewayRoute;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.MessageType;
import tools.jackson.databind.ObjectMapper;

class RequestTransformerTest {

  @Test
  void transformsOpenAiMessagesAndOptionsToSpringAi() throws Exception {
    OpenAiRequestTransformer transformer = new OpenAiRequestTransformer();
    var body =
        new ObjectMapper()
            .readTree(
                """
                {
                  "model": "gpt-5",
                  "messages": [
                    {"role": "system", "content": "Be concise"},
                    {"role": "user", "content": "Hello"}
                  ],
                  "temperature": 0.2,
                  "max_tokens": 100,
                  "stop": ["END"]
                }
                """);

    TransformedChatRequest result =
        transformer.transform(new GatewayRoute("OPENAI", "chat.completions", false), body);

    assertThat(result.messages()).hasSize(2);
    assertThat(result.messages().get(0).getMessageType()).isEqualTo(MessageType.SYSTEM);
    assertThat(result.messages().get(1).getText()).isEqualTo("Hello");
    assertThat(result.chatOptions().getModel()).isEqualTo("gpt-5");
    assertThat(result.chatOptions().getTemperature()).isEqualTo(0.2);
    assertThat(result.chatOptions().getMaxTokens()).isEqualTo(100);
    assertThat(result.chatOptions().getStopSequences()).containsExactly("END");
  }
}
