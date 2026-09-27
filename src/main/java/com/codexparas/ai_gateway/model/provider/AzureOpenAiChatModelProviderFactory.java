package com.codexparas.ai_gateway.model.provider;

import com.codexparas.ai_gateway.model.ProviderModelConfiguration;
import com.openai.azure.AzureOpenAIServiceVersion;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Azure OpenAI uses Spring AI's OpenAI-compatible model until an Azure starter is added. */
@Component
public final class AzureOpenAiChatModelProviderFactory
    extends AbstractSpringAiChatModelProviderFactory {

  public AzureOpenAiChatModelProviderFactory(ObjectProvider<OpenAiChatModel> models) {
    super("AZURE", models::getIfAvailable);
  }

  @Override
  public OpenAiChatModel create(ProviderModelConfiguration configuration) {
    OpenAiChatOptions.Builder options =
        OpenAiChatOptions.builder()
            .model(configuration.model())
            .azure(true)
            .deploymentName(
                configuration.deployment() == null
                    ? configuration.model()
                    : configuration.deployment());
    if (configuration.url() != null) options.baseUrl(configuration.url());
    if (configuration.apiVersion() != null)
      options.azureOpenAIServiceVersion(
          AzureOpenAIServiceVersion.fromString(configuration.apiVersion()));
    String apiKey = configuration.authValue("apiKey", "api_key", "token");
    if (apiKey != null) options.apiKey(apiKey);
    return OpenAiChatModel.builder().options(options.build()).build();
  }
}
