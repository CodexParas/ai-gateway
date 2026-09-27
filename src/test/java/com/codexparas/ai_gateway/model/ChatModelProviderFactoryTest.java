package com.codexparas.ai_gateway.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.codexparas.ai_gateway.model.provider.OpenAiChatModelProviderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import tools.jackson.databind.ObjectMapper;

class ChatModelProviderFactoryTest {
  @Test
  void buildsOpenAiModelFromResolvedConfiguration() throws Exception {
    var configuration =
        new ProviderModelConfiguration(
            "OPENAI",
            "gpt-test",
            "https://example.test/v1",
            null,
            null,
            null,
            new ObjectMapper().readTree("{\"apiKey\":\"secret\"}"),
            null,
            null);

    OpenAiChatModel model =
        new OpenAiChatModelProviderFactory(
                mock(org.springframework.beans.factory.ObjectProvider.class))
            .create(configuration);

    assertThat(model.getOptions().getModel()).isEqualTo("gpt-test");
    assertThat(model.getOptions().getBaseUrl()).isEqualTo("https://example.test/v1");
    assertThat(model.getOptions().getApiKey()).isEqualTo("secret");
  }
}
