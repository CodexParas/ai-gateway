package com.codexparas.ai_gateway.model.provider;

import com.codexparas.ai_gateway.model.ProviderModelConfiguration;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class AnthropicChatModelProviderFactory
    extends AbstractSpringAiChatModelProviderFactory {

  public AnthropicChatModelProviderFactory(ObjectProvider<AnthropicChatModel> models) {
    super("ANTHROPIC", models::getIfAvailable);
  }

  @Override
  public AnthropicChatModel create(ProviderModelConfiguration configuration) {
    AnthropicChatOptions.Builder options =
        AnthropicChatOptions.builder().model(configuration.model());
    if (configuration.url() != null) options.baseUrl(configuration.url());
    String apiKey = configuration.authValue("apiKey", "api_key", "token");
    if (apiKey != null) options.apiKey(apiKey);
    return AnthropicChatModel.builder().options(options.build()).build();
  }
}
