package com.codexparas.ai_gateway.model.provider;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Azure OpenAI uses Spring AI's OpenAI-compatible model until an Azure starter is added. */
@Component
public final class AzureOpenAiChatModelProviderFactory
    extends AbstractSpringAiChatModelProviderFactory {

  public AzureOpenAiChatModelProviderFactory(ObjectProvider<OpenAiChatModel> models) {
    super("AZURE", models::getIfAvailable);
  }
}
