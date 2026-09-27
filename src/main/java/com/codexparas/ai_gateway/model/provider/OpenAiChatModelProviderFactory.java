package com.codexparas.ai_gateway.model.provider;

import com.codexparas.ai_gateway.model.ProviderModelConfiguration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class OpenAiChatModelProviderFactory extends AbstractSpringAiChatModelProviderFactory {

  public OpenAiChatModelProviderFactory(ObjectProvider<OpenAiChatModel> models) {
    super("OPENAI", models::getIfAvailable);
  }

  @Override
  public OpenAiChatModel create(ProviderModelConfiguration configuration) {
    OpenAiChatOptions.Builder options = OpenAiChatOptions.builder().model(configuration.model());
    if (configuration.url() != null) options.baseUrl(configuration.url());
    String apiKey = configuration.authValue("apiKey", "api_key", "token");
    if (apiKey != null) options.apiKey(apiKey);
    return OpenAiChatModel.builder().options(options.build()).build();
  }
}
